package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.impl

import dev.remsely.f1goatdeterminer.datasync.domain.result.status.StatusPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1StatusFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.EntitySyncer
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.SyncResult
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.TransactionalPersistenceHelper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class StatusSyncer(
    private val statusFetcher: F1StatusFetcher,
    private val statusPersister: StatusPersister,
    private val checkpointPersister: SyncCheckpointPersister,
    private val txHelper: TransactionalPersistenceHelper,
) : EntitySyncer {

    override val entityType: SyncEntityType = SyncEntityType.STATUSES

    override fun sync(checkpoint: SyncCheckpoint): SyncResult {
        val checkpointId = requireNotNull(checkpoint.id) { "Checkpoint must be persisted" }
        var totalSynced = checkpoint.recordsSynced
        var lastOffset = checkpoint.lastOffset

        val summary = statusFetcher.forEachPageOfStatuses(checkpoint.lastOffset) { page ->
            val pageSynced = if (page.items.isNotEmpty()) {
                txHelper.executeInTransaction { statusPersister.upsertAll(page.items) }
            } else {
                0
            }
            totalSynced += pageSynced
            lastOffset = page.nextOffset

            checkpointPersister.updateProgress(
                id = checkpointId,
                lastOffset = page.nextOffset,
                recordsSynced = totalSynced,
            )

            log.info {
                "   STATUSES -- [${page.pageNumber}/${page.totalPages}] " +
                    "$pageSynced saved, $totalSynced total"
            }
        }

        return SyncResult(
            recordsSynced = totalSynced,
            lastOffset = lastOffset,
            lastSeason = null,
            lastRound = null,
            apiCallsMade = summary.apiCalls,
        )
    }
}
