package dev.remsely.f1goatdeterminer.datasync.usecase.command

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobFinder
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.SyncOrchestrator
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Resumes a previously failed or paused sync job.
 * Finds the most recent resumable job and re-runs the orchestrator.
 *
 * The orchestrator will skip already completed checkpoints and resume
 * from where the job left off.
 */
@Service
class ResumeSyncCommand(
    private val syncOrchestrator: SyncOrchestrator,
    private val syncJobFinder: SyncJobFinder,
    private val syncJobPersister: SyncJobPersister,
) {

    fun execute(): Boolean {
        val resumableJobs = syncJobFinder.findByStatusIn(
            listOf(SyncStatus.FAILED, SyncStatus.PAUSED),
        )

        val jobToResume = resumableJobs.firstOrNull()
        if (jobToResume == null) {
            log.info { "No resumable sync jobs found" }
            return false
        }

        val jobId = requireNotNull(jobToResume.id)
        log.info { "Resuming sync job id=$jobId, type=${jobToResume.type}, status=${jobToResume.status}" }

        syncJobPersister.updateStatus(jobId, SyncStatus.PENDING)
        syncOrchestrator.execute(
            jobToResume.copy(status = SyncStatus.PENDING),
        )

        return true
    }
}
