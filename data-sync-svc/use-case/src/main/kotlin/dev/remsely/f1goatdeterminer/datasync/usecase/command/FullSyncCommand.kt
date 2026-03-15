package dev.remsely.f1goatdeterminer.datasync.usecase.command

import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJob
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.SyncOrchestrator
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

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
    private val syncJobPersister: SyncJobPersister,
) {

    fun execute() {
        val job = syncJobPersister.tryCreateJob(SyncJob.Type.FULL) ?: return

        log.info { ">> Created FULL sync job #${job.id}" }
        syncOrchestrator.execute(job)
    }
}
