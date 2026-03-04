package dev.remsely.f1goatdeterminer.datasync.usecase.command

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJob
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobFinder
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.SyncOrchestrator
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

/**
 * Initiates a full sync of all F1 data from Jolpica API.
 * Creates a new FULL SyncJob and delegates to the orchestrator.
 *
 * Should be used for the initial population of the database.
 */
@Service
class FullSyncCommand(
    private val syncOrchestrator: SyncOrchestrator,
    private val syncJobFinder: SyncJobFinder,
    private val syncJobPersister: SyncJobPersister,
) {

    fun execute() {
        val activeJob = syncJobFinder.findActiveByType(SyncJob.Type.FULL)
        if (activeJob != null) {
            log.warn { "Full sync job is already active: id=${activeJob.id}" }
            return
        }

        val now = LocalDateTime.now()
        val job = syncJobPersister.save(
            SyncJob(
                id = null,
                type = SyncJob.Type.FULL,
                status = SyncStatus.PENDING,
                startedAt = now,
                updatedAt = now,
                completedAt = null,
                errorMessage = null,
                totalRequests = 0,
                failedRequests = 0,
            ),
        )

        log.info { "Created full sync job: id=${job.id}" }
        syncOrchestrator.execute(job)
    }
}
