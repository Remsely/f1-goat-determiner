package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.impl

import dev.remsely.f1goatdeterminer.datasync.domain.constructor.ConstructorFinder
import dev.remsely.f1goatdeterminer.datasync.domain.driver.DriverFinder
import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrixFinder
import dev.remsely.f1goatdeterminer.datasync.domain.result.qualifying.QualifyingResult
import dev.remsely.f1goatdeterminer.datasync.domain.result.qualifying.QualifyingResultPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1QualifyingFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedQualifyingResult
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.EntitySyncer
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.SyncResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
class QualifyingSyncer(
    private val qualifyingFetcher: F1QualifyingFetcher,
    private val qualifyingResultPersister: QualifyingResultPersister,
    private val grandPrixFinder: GrandPrixFinder,
    private val driverFinder: DriverFinder,
    private val constructorFinder: ConstructorFinder,
) : EntitySyncer {

    override val entityType: SyncEntityType = SyncEntityType.QUALIFYING_RESULTS

    @Transactional
    override fun sync(checkpoint: SyncCheckpoint): SyncResult {
        val seasons = grandPrixFinder.findAllSeasons().sorted()
        val startSeason = checkpoint.lastSeason ?: seasons.firstOrNull() ?: return emptyResult(checkpoint)
        val startRound = if (checkpoint.lastSeason != null) (checkpoint.lastRound ?: 0) + 1 else 1

        val gpIdLookup = grandPrixFinder.findAllSeasonRoundToId()
        val driverIdLookup = driverFinder.findAllRefToId()
        val constructorIdLookup = constructorFinder.findAllRefToId()

        var totalSynced = checkpoint.recordsSynced
        var apiCalls = 0
        var lastSeason = startSeason
        var lastRound = 0

        for (season in seasons.filter { it >= startSeason }) {
            val maxRound = grandPrixFinder.findMaxRoundBySeason(season) ?: continue
            val fromRound = if (season == startSeason) startRound else 1

            for (round in fromRound..maxRound) {
                totalSynced += syncRound(season, round, gpIdLookup, driverIdLookup, constructorIdLookup)
                apiCalls++
                lastSeason = season
                lastRound = round
            }
        }

        log.info { "Synced qualifying results through season=$lastSeason, round=$lastRound" }

        return SyncResult(
            recordsSynced = totalSynced,
            lastOffset = 0,
            lastSeason = lastSeason,
            lastRound = lastRound,
            apiCallsMade = apiCalls,
        )
    }

    private fun syncRound(
        season: Int,
        round: Int,
        gpIdLookup: Map<Pair<Int, Int>, Int>,
        driverIdLookup: Map<String, Int>,
        constructorIdLookup: Map<String, Int>,
    ): Int {
        log.debug { "Fetching qualifying for season=$season, round=$round" }
        val fetched = qualifyingFetcher.fetchQualifying(season, round)

        val gpId = gpIdLookup[season to round]
            ?: error("GrandPrix not found for season=$season, round=$round")

        val domainResults = fetched.map { it.toDomain(gpId, driverIdLookup, constructorIdLookup) }

        return if (domainResults.isNotEmpty()) {
            qualifyingResultPersister.upsertAll(domainResults)
        } else {
            0
        }
    }

    private fun FetchedQualifyingResult.toDomain(
        gpId: Int,
        driverIdLookup: Map<String, Int>,
        constructorIdLookup: Map<String, Int>,
    ) = QualifyingResult(
        grandPrixId = gpId,
        driverId = driverIdLookup[driverRef] ?: error("Driver not found: $driverRef"),
        constructorId = constructorIdLookup[constructorRef] ?: error("Constructor not found: $constructorRef"),
        number = number,
        position = position,
        q1 = q1,
        q2 = q2,
        q3 = q3,
    )

    private fun emptyResult(checkpoint: SyncCheckpoint) = SyncResult(
        recordsSynced = checkpoint.recordsSynced,
        lastOffset = 0,
        lastSeason = null,
        lastRound = null,
        apiCallsMade = 0,
    )
}
