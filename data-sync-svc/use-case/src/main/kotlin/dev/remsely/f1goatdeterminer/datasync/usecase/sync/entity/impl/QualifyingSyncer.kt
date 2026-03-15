package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.impl

import dev.remsely.f1goatdeterminer.datasync.domain.constructor.ConstructorFinder
import dev.remsely.f1goatdeterminer.datasync.domain.driver.DriverFinder
import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrixFinder
import dev.remsely.f1goatdeterminer.datasync.domain.result.qualifying.QualifyingResult
import dev.remsely.f1goatdeterminer.datasync.domain.result.qualifying.QualifyingResultPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1QualifyingFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedQualifyingResult
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.EntitySyncer
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.SyncResult
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.TransactionalPersistenceHelper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class QualifyingSyncer(
    private val qualifyingFetcher: F1QualifyingFetcher,
    private val qualifyingResultPersister: QualifyingResultPersister,
    private val grandPrixFinder: GrandPrixFinder,
    private val driverFinder: DriverFinder,
    private val constructorFinder: ConstructorFinder,
    private val checkpointPersister: SyncCheckpointPersister,
    private val txHelper: TransactionalPersistenceHelper,
) : EntitySyncer {

    override val entityType: SyncEntityType = SyncEntityType.QUALIFYING_RESULTS

    override fun sync(checkpoint: SyncCheckpoint): SyncResult {
        val gpIdLookup = grandPrixFinder.findAllSeasonRoundToId()
        val driverIdLookup = driverFinder.findAllRefToId()
        val constructorIdLookup = constructorFinder.findAllRefToId()
        val checkpointId = requireNotNull(checkpoint.id) { "Checkpoint must be persisted" }

        var totalSynced = checkpoint.recordsSynced
        var lastSeason: Int? = null
        var lastRound = 0
        var lastOffset = checkpoint.lastOffset

        val summary = qualifyingFetcher.forEachPageOfQualifying(checkpoint.lastOffset) { page ->
            val domainResults = page.items.map { fetched ->
                fetched.toDomain(gpIdLookup, driverIdLookup, constructorIdLookup)
            }

            val pageSynced = if (domainResults.isNotEmpty()) {
                txHelper.executeInTransaction { qualifyingResultPersister.upsertAll(domainResults) }
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
                "   QUALIFYING_RESULTS -- [${page.pageNumber}/${page.totalPages}] " +
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

    private fun FetchedQualifyingResult.toDomain(
        gpIdLookup: Map<Pair<Int, Int>, Int>,
        driverIdLookup: Map<String, Int>,
        constructorIdLookup: Map<String, Int>,
    ) = QualifyingResult(
        grandPrixId = gpIdLookup[season to round]
            ?: error("GrandPrix not found for season=$season, round=$round"),
        driverId = driverIdLookup[driverRef] ?: error("Driver not found: $driverRef"),
        constructorId = constructorIdLookup[constructorRef]
            ?: error("Constructor not found: $constructorRef"),
        number = number,
        position = position,
        q1 = q1,
        q2 = q2,
        q3 = q3,
    )
}
