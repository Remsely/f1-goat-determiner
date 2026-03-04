package dev.remsely.f1goatdeterminer.datasync.usecase.port

import java.math.BigDecimal

/**
 * Port for fetching driver standings from an external data source.
 */
interface F1DriverStandingFetcher {
    fun fetchDriverStandings(season: Int, round: Int): List<FetchedDriverStanding>
}

/**
 * Driver standing data fetched from external source.
 */
data class FetchedDriverStanding(
    val season: Int,
    val round: Int,
    val driverRef: String,
    val points: BigDecimal,
    val position: Int,
    val positionText: String,
    val wins: Int,
)
