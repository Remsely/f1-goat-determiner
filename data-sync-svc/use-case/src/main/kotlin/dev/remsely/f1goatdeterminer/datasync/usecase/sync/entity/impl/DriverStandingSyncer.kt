package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.impl

import dev.remsely.f1goatdeterminer.datasync.domain.driver.DriverFinder
import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrixFinder
import dev.remsely.f1goatdeterminer.datasync.domain.standings.driver.DriverStanding
import dev.remsely.f1goatdeterminer.datasync.domain.standings.driver.DriverStandingPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1DriverStandingFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedDriverStanding
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PaginationSummary
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.TransactionalPersistenceHelper
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

@Component
class DriverStandingSyncer(
    private val driverStandingFetcher: F1DriverStandingFetcher,
    private val driverStandingPersister: DriverStandingPersister,
    private val driverFinder: DriverFinder,
    grandPrixFinder: GrandPrixFinder,
    checkpointPersister: SyncCheckpointPersister,
    txHelper: TransactionalPersistenceHelper,
) : AbstractStandingSyncer<DriverStanding, FetchedDriverStanding>(grandPrixFinder, checkpointPersister, txHelper) {

    override val log: KLogger = KotlinLogging.logger {}
    override val entityType: SyncEntityType = SyncEntityType.DRIVER_STANDINGS
    override val entityName: String = "driver standings"

    override fun forEachPageOfSeasonStandings(
        season: Int,
        startOffset: Int,
        onPage: (PageFetchResult<FetchedDriverStanding>) -> Unit,
    ): PaginationSummary = driverStandingFetcher.forEachPageOfSeasonDriverStandings(season, startOffset, onPage)

    override fun getSeason(fetched: FetchedDriverStanding): Int = fetched.season

    override fun getRound(fetched: FetchedDriverStanding): Int = fetched.round

    override fun buildParticipantIdLookup(): Map<String, Int> =
        driverFinder.findAllRefToId()

    override fun toDomain(
        fetched: FetchedDriverStanding,
        gpId: Int,
        participantIdLookup: Map<String, Int>,
    ) = DriverStanding(
        grandPrixId = gpId,
        driverId = participantIdLookup[fetched.driverRef]
            ?: error("Driver not found: ${fetched.driverRef}"),
        points = fetched.points,
        position = fetched.position,
        positionText = fetched.positionText,
        wins = fetched.wins,
    )

    override fun persistAll(standings: List<DriverStanding>): Int =
        driverStandingPersister.upsertAll(standings)
}
