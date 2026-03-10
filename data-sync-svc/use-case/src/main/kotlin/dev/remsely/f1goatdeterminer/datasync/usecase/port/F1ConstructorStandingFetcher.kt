package dev.remsely.f1goatdeterminer.datasync.usecase.port

import java.math.BigDecimal

/**
 * Port for fetching constructor standings from an external data source.
 * Standings must be fetched per-season (global endpoint does not exist in Jolpica API).
 */
interface F1ConstructorStandingFetcher {
    fun forEachPageOfSeasonConstructorStandings(
        season: Int,
        startOffset: Int = 0,
        onPage: (PageFetchResult<FetchedConstructorStanding>) -> Unit,
    ): PaginationSummary
}

/**
 * Constructor standing data fetched from external source.
 */
data class FetchedConstructorStanding(
    val season: Int,
    val round: Int,
    val constructorRef: String,
    val points: BigDecimal,
    val position: Int?,
    val positionText: String,
    val wins: Int,
)
