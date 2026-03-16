package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.impl

import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrixFinder
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PaginationSummary
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.EntitySyncer
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.SyncResult
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.TransactionalPersistenceHelper
import io.github.oshai.kotlinlogging.KLogger

/**
 * Abstract base for standings syncers (driver standings, constructor standings).
 *
 * Standings must be fetched per-season because the Jolpica API does not provide
 * a global standings endpoint. This syncer iterates over all known seasons from the DB,
 * fetches standings for each season page by page, and persists them per round.
 *
 * @param D the domain standing type
 * @param F the fetched DTO type
 */
abstract class AbstractStandingSyncer<D, F>(
    private val grandPrixFinder: GrandPrixFinder,
    private val checkpointPersister: SyncCheckpointPersister,
    private val txHelper: TransactionalPersistenceHelper,
) : EntitySyncer {

    protected abstract val log: KLogger

    /** Human-readable entity name for log messages (e.g. "driver standings"). */
    protected abstract val entityName: String

    /** Fetch standings for a single season page by page, calling [onPage] for each page. */
    protected abstract fun forEachPageOfSeasonStandings(
        season: Int,
        startOffset: Int = 0,
        onPage: (PageFetchResult<F>) -> Unit,
    ): PaginationSummary

    /** Extract the season number from a fetched DTO. */
    protected abstract fun getSeason(fetched: F): Int

    /** Extract the round number from a fetched DTO. */
    protected abstract fun getRound(fetched: F): Int

    /** Build ref→id lookup for the participant (driver or constructor). */
    protected abstract fun buildParticipantIdLookup(): Map<String, Int>

    /** Map a fetched DTO to a domain object using the resolved GP id and participant lookup. */
    protected abstract fun toDomain(fetched: F, gpId: Int, participantIdLookup: Map<String, Int>): D

    /** Persist a batch of domain objects and return the number of upserted rows. */
    protected abstract fun persistAll(standings: List<D>): Int

    override fun sync(checkpoint: SyncCheckpoint): SyncResult {
        val gpIdLookup = grandPrixFinder.findAllSeasonRoundToId()
        val participantIdLookup = buildParticipantIdLookup()
        val checkpointId = requireNotNull(checkpoint.id) { "Checkpoint must be persisted" }

        val seasons = grandPrixFinder.findAllSeasons()
        val resumeSeason = checkpoint.lastSeason

        var totalSynced = checkpoint.recordsSynced
        var lastSeason: Int? = resumeSeason
        var lastRound = checkpoint.lastRound ?: 0
        var lastOffset = checkpoint.lastOffset
        var totalApiCalls = 0

        for (season in seasons) {
            if (resumeSeason != null && season < resumeSeason) continue

            // Always start from offset 0: the Jolpica standings endpoint returns only
            // the latest round's data with total = number of drivers. Re-fetching from 0
            // lets us detect when a new round has been completed.
            val seasonStartOffset = 0

            val summary = forEachPageOfSeasonStandings(season, seasonStartOffset) { page ->
                val bySeason = page.items.groupBy { getSeason(it) }

                for ((s, items) in bySeason.entries.sortedBy { it.key }) {
                    val byRound = items.groupBy { getRound(it) }

                    for ((round, roundItems) in byRound.entries.sortedBy { it.key }) {
                        val gpId = gpIdLookup[s to round] ?: continue

                        val domainStandings = roundItems.map { toDomain(it, gpId, participantIdLookup) }
                        val roundSynced = if (domainStandings.isNotEmpty()) {
                            txHelper.executeInTransaction { persistAll(domainStandings) }
                        } else {
                            0
                        }

                        totalSynced += roundSynced
                        lastRound = round
                    }
                }

                lastSeason = season
                lastOffset = page.nextOffset

                checkpointPersister.updateProgress(
                    id = checkpointId,
                    lastOffset = page.nextOffset,
                    lastSeason = season,
                    lastRound = lastRound,
                    recordsSynced = totalSynced,
                )

                log.info {
                    "   ${entityType.name} season=$season -- " +
                        "[${page.pageNumber}/${page.totalPages}] " +
                        "${page.items.size} fetched, $totalSynced total saved"
                }
            }

            totalApiCalls += summary.apiCalls
        }

        return SyncResult(
            recordsSynced = totalSynced,
            lastOffset = lastOffset,
            lastSeason = lastSeason,
            lastRound = lastRound,
            apiCallsMade = totalApiCalls,
        )
    }
}
