package dev.remsely.f1goatdeterminer.datasync.domain.result.race

import java.math.BigDecimal

private const val PODIUM_POSITION = 3

data class RaceResult(
    val id: Int,
    val grandPrixId: Int,
    val driverId: Int,
    val constructorId: Int,
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
    val statusId: Int,
) {
    val isClassified: Boolean
        get() = position != null

    val isWin: Boolean
        get() = position == 1

    val isPodium: Boolean
        get() = position != null && position <= PODIUM_POSITION
}
