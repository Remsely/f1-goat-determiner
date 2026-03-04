package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.impl

import dev.remsely.f1goatdeterminer.datasync.domain.result.status.StatusPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1StatusFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.EntitySyncer
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.SyncResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
class StatusSyncer(
    private val statusFetcher: F1StatusFetcher,
    private val statusPersister: StatusPersister,
) : EntitySyncer {

    override val entityType: SyncEntityType = SyncEntityType.STATUSES

    @Transactional
    override fun sync(checkpoint: SyncCheckpoint): SyncResult {
        log.info { "Syncing statuses from offset=${checkpoint.lastOffset}" }

        val statuses = statusFetcher.fetchAll(startOffset = checkpoint.lastOffset)

        val upserted = if (statuses.isNotEmpty()) {
            statusPersister.upsertAll(statuses)
        } else {
            0
        }

        log.info { "Synced $upserted statuses" }

        return SyncResult(
            recordsSynced = checkpoint.recordsSynced + upserted,
            lastOffset = checkpoint.lastOffset + statuses.size,
            lastSeason = null,
            lastRound = null,
            apiCallsMade = 1,
        )
    }
}
