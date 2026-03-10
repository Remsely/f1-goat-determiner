package dev.remsely.f1goatdeterminer.datasync.usecase.command

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointFinder
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJob
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobFinder
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.SyncOrchestrator
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Initiates an incremental sync of F1 data from Jolpica API.
 * Creates a new INCREMENTAL SyncJob with checkpoints seeded from the last completed job's offsets,
 * so only new data since the last sync is fetched.
 */
@Service
class IncrementalSyncCommand(
    private val syncOrchestrator: SyncOrchestrator,
    private val syncJobFinder: SyncJobFinder,
    private val syncJobPersister: SyncJobPersister,
    private val checkpointFinder: SyncCheckpointFinder,
    private val checkpointPersister: SyncCheckpointPersister,
) {

    fun execute() {
        val job = syncJobPersister.tryCreateJob(SyncJob.Type.INCREMENTAL) ?: return
        val jobId = requireNotNull(job.id)

        seedCheckpointsFromPreviousJob(jobId)

        log.info { ">> Created INCREMENTAL sync job #$jobId" }
        syncOrchestrator.execute(job)
    }

    private fun seedCheckpointsFromPreviousJob(jobId: Long) {
        val lastCompleted = syncJobFinder.findLatestCompleted()
        val previousCheckpoints = lastCompleted?.id
            ?.let { checkpointFinder.findByJobId(it) }
            ?.filter { it.isCompleted }
            ?.associateBy { it.entityType }
            .orEmpty()

        val newCheckpoints = SyncEntityType.syncOrdered.map { entityType ->
            val previous = previousCheckpoints[entityType]
            if (previous != null) {
                SyncCheckpoint.initFromPrevious(jobId, previous)
            } else {
                SyncCheckpoint.initPending(jobId, entityType)
            }
        }

        checkpointPersister.saveAll(newCheckpoints)
    }
}
