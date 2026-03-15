package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.impl

import dev.remsely.f1goatdeterminer.datasync.domain.constructor.ConstructorFinder
import dev.remsely.f1goatdeterminer.datasync.domain.driver.DriverFinder
import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrixFinder
import dev.remsely.f1goatdeterminer.datasync.domain.result.race.RaceResult
import dev.remsely.f1goatdeterminer.datasync.domain.result.race.RaceResultPersister
import dev.remsely.f1goatdeterminer.datasync.domain.result.status.StatusFinder
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1RaceResultFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedRaceResult
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.EntitySyncer
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.SyncResult
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.TransactionalPersistenceHelper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class RaceResultSyncer(
    private val raceResultFetcher: F1RaceResultFetcher,
    private val raceResultPersister: RaceResultPersister,
    private val grandPrixFinder: GrandPrixFinder,
    private val driverFinder: DriverFinder,
    private val constructorFinder: ConstructorFinder,
    private val statusFinder: StatusFinder,
    private val checkpointPersister: SyncCheckpointPersister,
    private val txHelper: TransactionalPersistenceHelper,
) : EntitySyncer {

    override val entityType: SyncEntityType = SyncEntityType.RACE_RESULTS

    override fun sync(checkpoint: SyncCheckpoint): SyncResult {
        val gpIdLookup = grandPrixFinder.findAllSeasonRoundToId()
        val driverIdLookup = driverFinder.findAllRefToId()
        val constructorIdLookup = constructorFinder.findAllRefToId()
        val statusIdLookup = statusFinder.findAllStatusToId()
        val checkpointId = requireNotNull(checkpoint.id) { "Checkpoint must be persisted" }

        var totalSynced = checkpoint.recordsSynced
        var lastSeason: Int? = null
        var lastRound = 0
        var lastOffset = checkpoint.lastOffset

        val summary = raceResultFetcher.forEachPageOfResults(checkpoint.lastOffset) { page ->
            val domainResults = page.items.map { fetched ->
                fetched.toDomain(gpIdLookup, driverIdLookup, constructorIdLookup, statusIdLookup)
            }

            val pageSynced = if (domainResults.isNotEmpty()) {
                txHelper.executeInTransaction { raceResultPersister.upsertAll(domainResults) }
            } else {
                0
            }

            totalSynced += pageSynced
            lastOffset = page.nextOffset

            if (page.items.isNotEmpty()) {
                lastSeason = page.items.maxOf { it.season }
                lastRound = page.items.filter { it.season == lastSeason }.maxOf { it.round }
            }

            checkpointPersister.updateProgress(
                id = checkpointId,
                lastOffset = page.nextOffset,
                lastSeason = lastSeason,
                lastRound = lastRound,
                recordsSynced = totalSynced,
            )

            log.info {
                "   RACE_RESULTS -- [${page.pageNumber}/${page.totalPages}] " +
                    "$pageSynced saved, $totalSynced total"
            }
        }

        return SyncResult(
            recordsSynced = totalSynced,
            lastOffset = lastOffset,
            lastSeason = lastSeason,
            lastRound = lastRound,
            apiCallsMade = summary.apiCalls,
        )
    }

    private fun FetchedRaceResult.toDomain(
        gpIdLookup: Map<Pair<Int, Int>, Int>,
        driverIdLookup: Map<String, Int>,
        constructorIdLookup: Map<String, Int>,
        statusIdLookup: Map<String, Int>,
    ) = RaceResult(
        grandPrixId = gpIdLookup[season to round]
            ?: error("GrandPrix not found for season=$season, round=$round"),
        driverId = driverIdLookup[driverRef] ?: error("Driver not found: $driverRef"),
        constructorId = constructorIdLookup[constructorRef]
            ?: error("Constructor not found: $constructorRef"),
        statusId = statusIdLookup[statusText] ?: error("Status not found: $statusText"),
        number = number,
        grid = grid,
        position = position,
        positionText = positionText,
        positionOrder = positionOrder,
        points = points,
        laps = laps,
        time = time,
        milliseconds = milliseconds,
        fastestLap = fastestLap,
        fastestLapRank = fastestLapRank,
        fastestLapTime = fastestLapTime,
        fastestLapSpeed = fastestLapSpeed,
    )
}
