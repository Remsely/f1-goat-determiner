package dev.remsely.f1goatdeterminer.datasync.usecase.command

import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.SyncOrchestrator
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

/**
 * Resumes a previously failed or paused sync job.
 * Finds the most recent resumable job and re-runs the orchestrator.
 *
 * The orchestrator will skip already completed checkpoints and resume
 * from where the job left off.
 *
 * @param cooldown minimum time that must elapse since the job's last update before it
 *   can be claimed. This prevents immediate retries after rate limit exhaustion.
 */
@Service
class ResumeSyncCommand(
    private val syncOrchestrator: SyncOrchestrator,
    private val syncJobPersister: SyncJobPersister,
) {

    fun execute(cooldown: Duration = Duration.ZERO): Boolean {
        val updatedBefore = LocalDateTime.now().minus(cooldown)
        val claimedJob = syncJobPersister.tryClaimResumableJob(updatedBefore)
        if (claimedJob == null) {
            log.info { "-- No resumable sync jobs found" }
            return false
        }

        val jobId = requireNotNull(claimedJob.id)
        log.info {
            ">> Resuming ${claimedJob.type} sync job #$jobId (claimed as PENDING)"
        }

        syncOrchestrator.execute(claimedJob)
        return true
    }
}
