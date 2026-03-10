package dev.remsely.f1goatdeterminer.datasync.usecase.port

import java.math.BigDecimal

/**
 * Port for fetching driver standings from an external data source.
 * Standings must be fetched per-season (global endpoint does not exist in Jolpica API).
 */
interface F1DriverStandingFetcher {
    fun forEachPageOfSeasonDriverStandings(
        season: Int,
        startOffset: Int = 0,
        onPage: (PageFetchResult<FetchedDriverStanding>) -> Unit,
    ): PaginationSummary
}

/**
 * Driver standing data fetched from external source.
 */
data class FetchedDriverStanding(
    val season: Int,
    val round: Int,
    val driverRef: String,
    val points: BigDecimal,
    val position: Int?,
    val positionText: String,
    val wins: Int,
)
