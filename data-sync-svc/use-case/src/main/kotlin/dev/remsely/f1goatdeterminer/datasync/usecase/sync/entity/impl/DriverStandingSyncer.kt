package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.impl

import dev.remsely.f1goatdeterminer.datasync.domain.driver.DriverFinder
import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrixFinder
import dev.remsely.f1goatdeterminer.datasync.domain.standings.driver.DriverStanding
import dev.remsely.f1goatdeterminer.datasync.domain.standings.driver.DriverStandingPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1DriverStandingFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedDriverStanding
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

@Component
class DriverStandingSyncer(
    private val driverStandingFetcher: F1DriverStandingFetcher,
    private val driverStandingPersister: DriverStandingPersister,
    private val driverFinder: DriverFinder,
    grandPrixFinder: GrandPrixFinder,
) : AbstractStandingSyncer<DriverStanding, FetchedDriverStanding>(grandPrixFinder) {

    override val log: KLogger = KotlinLogging.logger {}
    override val entityType: SyncEntityType = SyncEntityType.DRIVER_STANDINGS
    override val entityName: String = "driver standings"

    override fun fetchStandings(season: Int, round: Int): List<FetchedDriverStanding> =
        driverStandingFetcher.fetchDriverStandings(season, round)

    override fun buildParticipantIdLookup(): Map<String, Int> =
        driverFinder.findAllRefToId()

    override fun toDomain(
        fetched: FetchedDriverStanding,
        gpId: Int,
        participantIdLookup: Map<String, Int>,
    ) = DriverStanding(
        grandPrixId = gpId,
        driverId = participantIdLookup[fetched.driverRef] ?: error("Driver not found: ${fetched.driverRef}"),
        points = fetched.points,
        position = fetched.position,
        positionText = fetched.positionText,
        wins = fetched.wins,
    )

    override fun persistAll(standings: List<DriverStanding>): Int =
        driverStandingPersister.upsertAll(standings)
}
