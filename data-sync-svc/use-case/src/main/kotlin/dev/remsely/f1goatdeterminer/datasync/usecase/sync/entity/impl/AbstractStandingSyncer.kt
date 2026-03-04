package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.impl

import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrixFinder
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.EntitySyncer
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.SyncResult
import io.github.oshai.kotlinlogging.KLogger
import org.springframework.transaction.annotation.Transactional

/**
 * Abstract base for standings syncers (driver standings, constructor standings).
 *
 * Subclasses only need to provide entity-specific fetch, mapping, and persist logic.
 *
 * @param D the domain standing type
 * @param F the fetched DTO type
 */
abstract class AbstractStandingSyncer<D, F>(
    private val grandPrixFinder: GrandPrixFinder,
) : EntitySyncer {

    protected abstract val log: KLogger

    /** Human-readable entity name for log messages (e.g. "driver standings"). */
    protected abstract val entityName: String

    /** Fetch standings from the external API for a specific season/round. */
    protected abstract fun fetchStandings(season: Int, round: Int): List<F>

    /** Build ref→id lookup for the participant (driver or constructor). */
    protected abstract fun buildParticipantIdLookup(): Map<String, Int>

    /** Map a fetched DTO to a domain object using the resolved GP id and participant lookup. */
    protected abstract fun toDomain(fetched: F, gpId: Int, participantIdLookup: Map<String, Int>): D

    /** Persist a batch of domain objects and return the number of upserted rows. */
    protected abstract fun persistAll(standings: List<D>): Int

    @Transactional
    override fun sync(checkpoint: SyncCheckpoint): SyncResult {
        val seasons = grandPrixFinder.findAllSeasons().sorted()
        val startSeason = checkpoint.lastSeason ?: seasons.firstOrNull() ?: return emptyResult(checkpoint)
        val startRound = if (checkpoint.lastSeason != null) (checkpoint.lastRound ?: 0) + 1 else 1

        val gpIdLookup = grandPrixFinder.findAllSeasonRoundToId()
        val participantIdLookup = buildParticipantIdLookup()

        var totalSynced = checkpoint.recordsSynced
        var apiCalls = 0
        var lastSeason = startSeason
        var lastRound = 0

        for (season in seasons.filter { it >= startSeason }) {
            val maxRound = grandPrixFinder.findMaxRoundBySeason(season) ?: continue
            val fromRound = if (season == startSeason) startRound else 1

            for (round in fromRound..maxRound) {
                totalSynced += syncRound(season, round, gpIdLookup, participantIdLookup)
                apiCalls++
                lastSeason = season
                lastRound = round
            }
        }

        log.info { "Synced $entityName through season=$lastSeason, round=$lastRound" }

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
        participantIdLookup: Map<String, Int>,
    ): Int {
        log.debug { "Fetching $entityName for season=$season, round=$round" }
        val fetched = fetchStandings(season, round)

        val gpId = gpIdLookup[season to round]
            ?: error("GrandPrix not found for season=$season, round=$round")

        val domainStandings = fetched.map { toDomain(it, gpId, participantIdLookup) }

        return if (domainStandings.isNotEmpty()) {
            persistAll(domainStandings)
        } else {
            0
        }
    }

    private fun emptyResult(checkpoint: SyncCheckpoint) = SyncResult(
        recordsSynced = checkpoint.recordsSynced,
        lastOffset = 0,
        lastSeason = null,
        lastRound = null,
        apiCallsMade = 0,
    )
}
