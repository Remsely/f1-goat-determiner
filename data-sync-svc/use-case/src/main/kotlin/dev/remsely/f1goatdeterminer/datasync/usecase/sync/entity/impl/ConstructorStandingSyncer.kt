package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.impl

import dev.remsely.f1goatdeterminer.datasync.domain.constructor.ConstructorFinder
import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrixFinder
import dev.remsely.f1goatdeterminer.datasync.domain.standings.constructor.ConstructorStanding
import dev.remsely.f1goatdeterminer.datasync.domain.standings.constructor.ConstructorStandingPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1ConstructorStandingFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedConstructorStanding
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PaginationSummary
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.TransactionalPersistenceHelper
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

@Component
class ConstructorStandingSyncer(
    private val constructorStandingFetcher: F1ConstructorStandingFetcher,
    private val constructorStandingPersister: ConstructorStandingPersister,
    private val constructorFinder: ConstructorFinder,
    grandPrixFinder: GrandPrixFinder,
    checkpointPersister: SyncCheckpointPersister,
    txHelper: TransactionalPersistenceHelper,
) : AbstractStandingSyncer<ConstructorStanding, FetchedConstructorStanding>(
    grandPrixFinder,
    checkpointPersister,
    txHelper,
) {

    override val log: KLogger = KotlinLogging.logger {}
    override val entityType: SyncEntityType = SyncEntityType.CONSTRUCTOR_STANDINGS
    override val entityName: String = "constructor standings"

    override fun forEachPageOfSeasonStandings(
        season: Int,
        startOffset: Int,
        onPage: (PageFetchResult<FetchedConstructorStanding>) -> Unit,
    ): PaginationSummary =
        constructorStandingFetcher.forEachPageOfSeasonConstructorStandings(season, startOffset, onPage)

    override fun getSeason(fetched: FetchedConstructorStanding): Int = fetched.season

    override fun getRound(fetched: FetchedConstructorStanding): Int = fetched.round

    override fun buildParticipantIdLookup(): Map<String, Int> =
        constructorFinder.findAllRefToId()

    override fun toDomain(
        fetched: FetchedConstructorStanding,
        gpId: Int,
        participantIdLookup: Map<String, Int>,
    ) = ConstructorStanding(
        grandPrixId = gpId,
        constructorId = participantIdLookup[fetched.constructorRef]
            ?: error("Constructor not found: ${fetched.constructorRef}"),
        points = fetched.points,
        position = fetched.position,
        positionText = fetched.positionText,
        wins = fetched.wins,
    )

    override fun persistAll(standings: List<ConstructorStanding>): Int =
        constructorStandingPersister.upsertAll(standings)
}
