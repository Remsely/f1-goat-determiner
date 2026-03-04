package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.impl

import dev.remsely.f1goatdeterminer.datasync.domain.circuit.CircuitFinder
import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrix
import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrixPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1GrandPrixFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1SeasonFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.EntitySyncer
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.SyncResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
class GrandPrixSyncer(
    private val seasonFetcher: F1SeasonFetcher,
    private val grandPrixFetcher: F1GrandPrixFetcher,
    private val grandPrixPersister: GrandPrixPersister,
    private val circuitFinder: CircuitFinder,
) : EntitySyncer {

    override val entityType: SyncEntityType = SyncEntityType.GRAND_PRIX

    @Transactional
    override fun sync(checkpoint: SyncCheckpoint): SyncResult {
        val seasons = seasonFetcher.fetchAllSeasons()
        val startSeason = checkpoint.lastSeason ?: seasons.firstOrNull() ?: return emptyResult(checkpoint)
        val circuitRefToId = circuitFinder.findAllRefToId()

        var totalSynced = checkpoint.recordsSynced
        var apiCalls = 1
        var lastSeason = startSeason
        var lastRound = checkpoint.lastRound ?: 0

        for (season in seasons.filter { it >= startSeason }) {
            log.info { "Syncing Grand Prix for season $season" }

            val fetchedRaces = grandPrixFetcher.fetchRaces(season)
            apiCalls++

            val domainRaces = fetchedRaces.map { fetched ->
                val circuitId = circuitRefToId[fetched.circuitRef]
                    ?: error("Circuit not found for ref: ${fetched.circuitRef}")

                GrandPrix(
                    season = fetched.season,
                    round = fetched.round,
                    circuitId = circuitId,
                    name = fetched.name,
                    date = fetched.date,
                    time = fetched.time,
                )
            }

            if (domainRaces.isNotEmpty()) {
                totalSynced += grandPrixPersister.upsertAll(domainRaces)
            }

            lastSeason = season
            lastRound = fetchedRaces.maxOfOrNull { it.round } ?: 0
        }

        log.info { "Synced Grand Prix through season $lastSeason" }

        return SyncResult(
            recordsSynced = totalSynced,
            lastOffset = 0,
            lastSeason = lastSeason,
            lastRound = lastRound,
            apiCallsMade = apiCalls,
        )
    }

    private fun emptyResult(checkpoint: SyncCheckpoint) = SyncResult(
        recordsSynced = checkpoint.recordsSynced,
        lastOffset = 0,
        lastSeason = null,
        lastRound = null,
        apiCallsMade = 1,
    )
}
