package dev.remsely.f1goatdeterminer.datasync.usecase.sync

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointFinder
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJob
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.EntitySyncerRegistry
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.SyncResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import kotlin.time.Duration
import kotlin.time.measureTimedValue
import kotlin.time.toKotlinDuration

private val log = KotlinLogging.logger {}

/**
 * Orchestrates the synchronization of all entity types for a given [SyncJob].
 *
 * Iterates through each [SyncEntityType] in sync order, delegating to the appropriate
 * [dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.EntitySyncer].
 * Updates checkpoints and job status as sync progresses.
 *
 * Handles errors per entity type: if one entity fails, it marks the checkpoint as FAILED
 * and continues to the next entity type. At the end, the job is marked COMPLETED only if
 * all checkpoints succeeded, otherwise FAILED.
 */
@Service
class SyncOrchestrator(
    private val syncerRegistry: EntitySyncerRegistry,
    private val checkpointPersister: SyncCheckpointPersister,
    private val checkpointFinder: SyncCheckpointFinder,
    private val jobPersister: SyncJobPersister,
) {

    /**
     * Runs the sync process for the given job.
     * Creates checkpoints for all entity types if they don't exist, then processes them in order.
     *
     * Uses [runCatching] to guarantee that the job status is ALWAYS updated,
     * even if an unexpected exception occurs. This prevents jobs from being stuck in IN_PROGRESS forever.
     */
    fun execute(job: SyncJob) {
        val jobId = requireNotNull(job.id) { "SyncJob must be persisted before orchestration" }
        val totalSteps = SyncEntityType.syncOrdered.size

        log.info { ">> ${job.type} sync job #$jobId started ($totalSteps steps)" }
        jobPersister.updateStatus(jobId, SyncStatus.IN_PROGRESS)

        var totalRequests = 0
        var failedRequests = 0
        var completedSteps = 0
        var hasFailures = false
        val failedEntityTypes = mutableSetOf<SyncEntityType>()

        val (_, jobElapsed) = measureTimedValue {
            runCatching {
                val checkpoints = ensureCheckpoints(jobId)

                for ((index, checkpoint) in checkpoints.withIndex()) {
                    val step = index + 1
                    val result = executeCheckpoint(
                        jobId,
                        checkpoint,
                        step,
                        totalSteps,
                        failedEntityTypes,
                    )
                    totalRequests += result.apiCalls
                    failedRequests += result.failures
                    hasFailures = hasFailures || result.failed
                    if (result.failed) {
                        failedEntityTypes += checkpoint.entityType
                    } else {
                        completedSteps++
                    }
                    jobPersister.updateProgress(jobId, totalRequests, failedRequests)
                    jobPersister.touchUpdatedAt(jobId)
                    if (result.rateLimitExhausted) break
                }
            }.onFailure { e ->
                hasFailures = true
                log.error(e) {
                    "!! ${job.type} sync job #$jobId unexpected error: ${e.message}"
                }
                jobPersister.updateStatus(jobId, SyncStatus.FAILED, e.message)
            }
        }

        val finalStatus = if (hasFailures) SyncStatus.FAILED else SyncStatus.COMPLETED
        jobPersister.complete(jobId, finalStatus)

        val marker = if (hasFailures) "XX" else "OK"
        log.info {
            "$marker ${job.type} sync job #$jobId ${finalStatus.name} " +
                "-- $completedSteps/$totalSteps steps, $totalRequests API calls, " +
                "$failedRequests failed (${jobElapsed.formatPretty()})"
        }
    }

    private data class SyncStepResult(
        val apiCalls: Int = 0,
        val failures: Int = 0,
        val failed: Boolean = false,
        val rateLimitExhausted: Boolean = false,
    )

    private fun executeCheckpoint(
        jobId: Long,
        checkpoint: SyncCheckpoint,
        step: Int,
        totalSteps: Int,
        failedEntityTypes: Set<SyncEntityType>,
    ): SyncStepResult {
        val skipResult = evaluateSkip(checkpoint, step, totalSteps, failedEntityTypes)
        if (skipResult != null) return skipResult

        val syncResult = syncSingleCheckpoint(checkpoint, step, totalSteps)

        if (syncResult.rateLimitExhausted) {
            log.warn {
                "!! Rate limit exhausted — stopping sync job #$jobId to avoid further 429s"
            }
        }
        return syncResult
    }

    private fun evaluateSkip(
        checkpoint: SyncCheckpoint,
        step: Int,
        totalSteps: Int,
        failedEntityTypes: Set<SyncEntityType>,
    ): SyncStepResult? {
        if (checkpoint.isCompleted) {
            log.info { "-- [$step/$totalSteps] ${checkpoint.entityType} -- skipped (already completed)" }
            return SyncStepResult()
        }

        if (checkpoint.status == SyncStatus.FAILED && !checkpoint.isRetryable) {
            log.warn {
                "-- [$step/$totalSteps] ${checkpoint.entityType} -- skipped " +
                    "(non-retryable, retryCount=${checkpoint.retryCount})"
            }
            return SyncStepResult(failed = true)
        }

        val failedDeps = checkpoint.entityType.dependencies.intersect(failedEntityTypes)
        if (failedDeps.isNotEmpty()) {
            checkpoint.id?.let { checkpointId ->
                checkpointPersister.updateStatus(
                    checkpointId,
                    SyncStatus.SKIPPED,
                    "Dependency failed: $failedDeps",
                )
            }
            log.warn {
                "-- [$step/$totalSteps] ${checkpoint.entityType} -- skipped " +
                    "(dependency failed: $failedDeps)"
            }
            return SyncStepResult(failed = true)
        }

        return null
    }

    private fun syncSingleCheckpoint(
        checkpoint: SyncCheckpoint,
        step: Int,
        totalSteps: Int,
    ): SyncStepResult {
        val checkpointId = requireNotNull(checkpoint.id) { "Checkpoint must be persisted" }
        val entity = checkpoint.entityType

        log.info { ">> [$step/$totalSteps] $entity -- syncing" }
        checkpointPersister.updateStatus(checkpointId, SyncStatus.IN_PROGRESS)

        val syncer = syncerRegistry.getSyncer(entity)

        val (result, elapsed) = measureTimedValue {
            runCatching { syncer.sync(checkpoint) }
        }

        return result.fold(
            onSuccess = { syncResult ->
                handleSyncSuccess(checkpointId, syncResult)
                log.info {
                    "OK [$step/$totalSteps] $entity -- " +
                        "${syncResult.recordsSynced} records, " +
                        "${syncResult.apiCallsMade} API calls (${elapsed.formatPretty()})"
                }
                SyncStepResult(apiCalls = syncResult.apiCallsMade)
            },
            onFailure = { e ->
                handleSyncFailure(checkpointId, entity, e)
                log.error(e) {
                    "XX [$step/$totalSteps] $entity -- FAILED: ${e.message} " +
                        "(${elapsed.formatPretty()})"
                }
                SyncStepResult(
                    failures = 1,
                    failed = true,
                    rateLimitExhausted = e is RateLimitExhaustedException,
                )
            },
        )
    }

    private fun handleSyncSuccess(
        checkpointId: Long,
        result: SyncResult,
    ) {
        checkpointPersister.completeWithProgress(
            id = checkpointId,
            lastOffset = result.lastOffset,
            lastSeason = result.lastSeason,
            lastRound = result.lastRound,
            recordsSynced = result.recordsSynced,
        )
    }

    private fun handleSyncFailure(
        checkpointId: Long,
        entityType: SyncEntityType,
        error: Throwable,
    ) {
        val message = "Failed to sync $entityType: ${error.message}"
        checkpointPersister.fail(checkpointId, message)

        // Don't increment retry count for rate limit errors — they are transient.
        // The cooldown on job resume is sufficient to prevent rapid 429 loops.
        if (error !is RateLimitExhaustedException) {
            checkpointPersister.incrementRetryCount(checkpointId)
        }
    }

    /**
     * Ensures checkpoints exist for all entity types in the correct sync order.
     * If no checkpoints exist for this job, creates them. Otherwise, returns existing ones
     * ordered by sync priority.
     *
     * When resuming, resets retry counts on FAILED checkpoints so they get a fresh
     * chance after the cooldown period has elapsed.
     */
    private fun ensureCheckpoints(jobId: Long): List<SyncCheckpoint> {
        val existing = checkpointFinder.findByJobId(jobId)

        if (existing.isNotEmpty()) {
            // Reset retry counts for FAILED checkpoints on resume
            existing.filter { it.status == SyncStatus.FAILED }
                .mapNotNull { it.id }
                .forEach { checkpointPersister.resetRetryCount(it) }

            return SyncEntityType.syncOrdered.mapNotNull { entityType ->
                existing.find { it.entityType == entityType }
                    ?.let { cp ->
                        if (cp.status == SyncStatus.FAILED) cp.copy(retryCount = 0) else cp
                    }
            }
        }

        val newCheckpoints = SyncEntityType.syncOrdered.map { entityType ->
            SyncCheckpoint.initPending(jobId, entityType)
        }

        return checkpointPersister.saveAll(newCheckpoints)
    }
}

internal fun Duration.formatPretty(): String {
    val totalSeconds = inWholeSeconds
    return when {
        totalSeconds < 1 -> "${inWholeMilliseconds}ms"
        totalSeconds < 60 -> "${totalSeconds}s"
        totalSeconds < 3600 -> "${totalSeconds / 60}m ${totalSeconds % 60}s"
        else -> "${totalSeconds / 3600}h ${(totalSeconds % 3600) / 60}m"
    }
}

internal fun java.time.Duration.formatPretty(): String = toKotlinDuration().formatPretty()
