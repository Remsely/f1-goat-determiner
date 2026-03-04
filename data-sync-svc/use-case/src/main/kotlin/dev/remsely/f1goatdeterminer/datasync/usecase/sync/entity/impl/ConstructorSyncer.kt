package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.impl

import dev.remsely.f1goatdeterminer.datasync.domain.constructor.ConstructorPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1ConstructorFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.EntitySyncer
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.SyncResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
class ConstructorSyncer(
    private val constructorFetcher: F1ConstructorFetcher,
    private val constructorPersister: ConstructorPersister,
) : EntitySyncer {

    override val entityType: SyncEntityType = SyncEntityType.CONSTRUCTORS

    @Transactional
    override fun sync(checkpoint: SyncCheckpoint): SyncResult {
        log.info { "Syncing constructors from offset=${checkpoint.lastOffset}" }

        val constructors = constructorFetcher.fetchAll(startOffset = checkpoint.lastOffset)

        val upserted = if (constructors.isNotEmpty()) {
            constructorPersister.upsertAll(constructors)
        } else {
            0
        }

        log.info { "Synced $upserted constructors" }

        return SyncResult(
            recordsSynced = checkpoint.recordsSynced + upserted,
            lastOffset = checkpoint.lastOffset + constructors.size,
            lastSeason = null,
            lastRound = null,
            apiCallsMade = 1,
        )
    }
}
