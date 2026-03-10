package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.impl

import dev.remsely.f1goatdeterminer.datasync.domain.circuit.CircuitPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1CircuitFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.EntitySyncer
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.SyncResult
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.TransactionalPersistenceHelper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class CircuitSyncer(
    private val circuitFetcher: F1CircuitFetcher,
    private val circuitPersister: CircuitPersister,
    private val checkpointPersister: SyncCheckpointPersister,
    private val txHelper: TransactionalPersistenceHelper,
) : EntitySyncer {

    override val entityType: SyncEntityType = SyncEntityType.CIRCUITS

    override fun sync(checkpoint: SyncCheckpoint): SyncResult {
        val checkpointId = requireNotNull(checkpoint.id) { "Checkpoint must be persisted" }
        var totalSynced = checkpoint.recordsSynced
        var lastOffset = checkpoint.lastOffset

        val summary = circuitFetcher.forEachPageOfCircuits(checkpoint.lastOffset) { page ->
            val pageSynced = if (page.items.isNotEmpty()) {
                txHelper.executeInTransaction { circuitPersister.upsertAll(page.items) }
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
                "   CIRCUITS -- [${page.pageNumber}/${page.totalPages}] " +
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
