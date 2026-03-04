package dev.remsely.f1goatdeterminer.datasync.usecase.port

import java.math.BigDecimal

/**
 * Port for fetching constructor standings from an external data source.
 */
interface F1ConstructorStandingFetcher {
    fun fetchConstructorStandings(season: Int, round: Int): List<FetchedConstructorStanding>
}

/**
 * Constructor standing data fetched from external source.
 */
data class FetchedConstructorStanding(
    val season: Int,
    val round: Int,
    val constructorRef: String,
    val points: BigDecimal,
    val position: Int,
    val positionText: String,
    val wins: Int,
)
