package dev.remsely.f1goatdeterminer.datasync.usecase.sync

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointFinder
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJob
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.EntitySyncer
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.EntitySyncerRegistry
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.SyncResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Orchestrates the synchronization of all entity types for a given [SyncJob].
 *
 * Iterates through each [SyncEntityType] in sync order, delegating to the appropriate
 * [EntitySyncer]. Updates checkpoints and job status as sync progresses.
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
     */
    fun execute(job: SyncJob) {
        val jobId = requireNotNull(job.id) { "SyncJob must be persisted before orchestration" }

        log.info { "Starting sync orchestration for job id=$jobId, type=${job.type}" }
        jobPersister.updateStatus(jobId, SyncStatus.IN_PROGRESS)

        val checkpoints = ensureCheckpoints(jobId)

        var totalRequests = 0
        var failedRequests = 0
        var hasFailures = false

        for (checkpoint in checkpoints) {
            if (checkpoint.isCompleted) {
                log.info { "Skipping already completed checkpoint: ${checkpoint.entityType}" }
                continue
            }

            val checkpointId = requireNotNull(checkpoint.id) { "Checkpoint must be persisted" }

            try {
                checkpointPersister.updateStatus(checkpointId, SyncStatus.IN_PROGRESS)

                val syncer = syncerRegistry.getSyncer(checkpoint.entityType)
                val result = runSyncer(syncer, checkpoint)

                totalRequests += result.apiCallsMade

                checkpointPersister.updateProgress(
                    id = checkpointId,
                    lastOffset = result.lastOffset,
                    lastSeason = result.lastSeason,
                    lastRound = result.lastRound,
                    recordsSynced = result.recordsSynced,
                )
                checkpointPersister.complete(checkpointId, result.recordsSynced)

                log.info {
                    "Completed sync for ${checkpoint.entityType}: " +
                        "${result.recordsSynced} records, ${result.apiCallsMade} API calls"
                }
            } catch (e: SyncException) {
                hasFailures = true
                failedRequests++

                log.error(e) { "Failed to sync ${checkpoint.entityType}: ${e.message}" }

                checkpointPersister.fail(checkpointId, e.message ?: "Unknown error")
                checkpointPersister.incrementRetryCount(checkpointId)
            }

            jobPersister.updateProgress(jobId, totalRequests, failedRequests)
        }

        val finalStatus = if (hasFailures) SyncStatus.FAILED else SyncStatus.COMPLETED
        jobPersister.complete(jobId, finalStatus)

        log.info {
            "Sync orchestration finished for job id=$jobId: " +
                "status=$finalStatus, totalRequests=$totalRequests, failedRequests=$failedRequests"
        }
    }

    /**
     * Ensures checkpoints exist for all entity types in the correct sync order.
     * If no checkpoints exist for this job, creates them. Otherwise, returns existing ones
     * ordered by sync priority.
     */
    private fun ensureCheckpoints(jobId: Long): List<SyncCheckpoint> {
        val existing = checkpointFinder.findByJobId(jobId)

        if (existing.isNotEmpty()) {
            return SyncEntityType.syncOrdered.mapNotNull { entityType ->
                existing.find { it.entityType == entityType }
            }
        }

        val newCheckpoints = SyncEntityType.syncOrdered.map { entityType ->
            SyncCheckpoint.initPending(jobId, entityType)
        }

        return checkpointPersister.saveAll(newCheckpoints)
    }

    private fun runSyncer(syncer: EntitySyncer, checkpoint: SyncCheckpoint): SyncResult = try {
        syncer.sync(checkpoint)
    } catch (e: SyncException) {
        throw e
    } catch (e: IllegalStateException) {
        throw SyncException("Sync failed for ${checkpoint.entityType}: ${e.message}", e)
    } catch (e: IllegalArgumentException) {
        throw SyncException("Sync failed for ${checkpoint.entityType}: ${e.message}", e)
    }
}
