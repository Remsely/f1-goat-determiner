package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.impl

import dev.remsely.f1goatdeterminer.datasync.domain.driver.DriverPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1DriverFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.EntitySyncer
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.SyncResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
class DriverSyncer(
    private val driverFetcher: F1DriverFetcher,
    private val driverPersister: DriverPersister,
) : EntitySyncer {

    override val entityType: SyncEntityType = SyncEntityType.DRIVERS

    @Transactional
    override fun sync(checkpoint: SyncCheckpoint): SyncResult {
        log.info { "Syncing drivers from offset=${checkpoint.lastOffset}" }

        val drivers = driverFetcher.fetchAll(startOffset = checkpoint.lastOffset)

        val upserted = if (drivers.isNotEmpty()) {
            driverPersister.upsertAll(drivers)
        } else {
            0
        }

        log.info { "Synced $upserted drivers" }

        return SyncResult(
            recordsSynced = checkpoint.recordsSynced + upserted,
            lastOffset = checkpoint.lastOffset + drivers.size,
            lastSeason = null,
            lastRound = null,
            apiCallsMade = 1,
        )
    }
}
