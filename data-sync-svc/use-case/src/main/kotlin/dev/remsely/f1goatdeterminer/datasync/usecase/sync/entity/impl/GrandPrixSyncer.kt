package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.impl

import dev.remsely.f1goatdeterminer.datasync.domain.circuit.CircuitFinder
import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrix
import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrixPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1GrandPrixFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedGrandPrix
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.EntitySyncer
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.SyncResult
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.TransactionalPersistenceHelper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GrandPrixSyncer(
    private val grandPrixFetcher: F1GrandPrixFetcher,
    private val grandPrixPersister: GrandPrixPersister,
    private val circuitFinder: CircuitFinder,
    private val checkpointPersister: SyncCheckpointPersister,
    private val txHelper: TransactionalPersistenceHelper,
) : EntitySyncer {

    override val entityType: SyncEntityType = SyncEntityType.GRAND_PRIX

    override fun sync(checkpoint: SyncCheckpoint): SyncResult {
        val circuitRefToId = circuitFinder.findAllRefToId()
        val checkpointId = requireNotNull(checkpoint.id) { "Checkpoint must be persisted" }

        var totalSynced = checkpoint.recordsSynced
        var lastSeason: Int? = null
        var lastRound = 0
        var lastOffset = checkpoint.lastOffset

        val summary = grandPrixFetcher.forEachPageOfRaces(checkpoint.lastOffset) { page ->
            val bySeason = page.items.groupBy { it.season }

            for ((season, races) in bySeason.entries.sortedBy { it.key }) {
                val domainRaces = races.map { it.toDomain(circuitRefToId) }
                val seasonSynced = if (domainRaces.isNotEmpty()) {
                    txHelper.executeInTransaction { grandPrixPersister.upsertAll(domainRaces) }
                } else {
                    0
                }
                totalSynced += seasonSynced
                lastSeason = season
                lastRound = races.maxOf { it.round }
            }

            lastOffset = page.nextOffset

            checkpointPersister.updateProgress(
                id = checkpointId,
                lastOffset = page.nextOffset,
                lastSeason = lastSeason,
                lastRound = lastRound,
                recordsSynced = totalSynced,
            )

            log.info {
                "   GRAND_PRIX -- [${page.pageNumber}/${page.totalPages}] " +
                    "${page.items.size} races fetched, $totalSynced total saved"
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

    private fun FetchedGrandPrix.toDomain(circuitRefToId: Map<String, Int>) = GrandPrix(
        season = season,
        round = round,
        circuitId = circuitRefToId[circuitRef]
            ?: error("Circuit not found for ref: $circuitRef"),
        name = name,
        date = date,
        time = time,
    )
}
