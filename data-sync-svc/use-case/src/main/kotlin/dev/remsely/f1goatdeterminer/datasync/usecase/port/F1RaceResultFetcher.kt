package dev.remsely.f1goatdeterminer.datasync.usecase.port

import java.math.BigDecimal

/**
 * Port for fetching race results from an external data source.
 */
interface F1RaceResultFetcher {
    fun fetchResults(season: Int, round: Int): List<FetchedRaceResult>
}

/**
 * Race result data fetched from external source.
 * References to driver, constructor, and status are identified by ref/name (not DB IDs).
 */
data class FetchedRaceResult(
    val season: Int,
    val round: Int,
    val driverRef: String,
    val constructorRef: String,
    val statusText: String,
    val number: Int?,
    val grid: Int,
    val position: Int?,
    val positionText: String,
    val positionOrder: Int,
    val points: BigDecimal,
    val laps: Int,
    val time: String?,
    val milliseconds: Long?,
    val fastestLap: Int?,
    val fastestLapRank: Int?,
    val fastestLapTime: String?,
    val fastestLapSpeed: BigDecimal?,
)
