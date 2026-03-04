package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.impl

import dev.remsely.f1goatdeterminer.datasync.domain.circuit.CircuitPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1CircuitFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.EntitySyncer
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.SyncResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
class CircuitSyncer(
    private val circuitFetcher: F1CircuitFetcher,
    private val circuitPersister: CircuitPersister,
) : EntitySyncer {

    override val entityType: SyncEntityType = SyncEntityType.CIRCUITS

    @Transactional
    override fun sync(checkpoint: SyncCheckpoint): SyncResult {
        log.info { "Syncing circuits from offset=${checkpoint.lastOffset}" }

        val circuits = circuitFetcher.fetchAll(startOffset = checkpoint.lastOffset)

        val upserted = if (circuits.isNotEmpty()) {
            circuitPersister.upsertAll(circuits)
        } else {
            0
        }

        log.info { "Synced $upserted circuits" }

        return SyncResult(
            recordsSynced = checkpoint.recordsSynced + upserted,
            lastOffset = checkpoint.lastOffset + circuits.size,
            lastSeason = null,
            lastRound = null,
            apiCallsMade = 1,
        )
    }
}
