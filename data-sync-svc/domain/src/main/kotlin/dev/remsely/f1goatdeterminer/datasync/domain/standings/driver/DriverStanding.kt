package dev.remsely.f1goatdeterminer.datasync.domain.standings.driver

import java.math.BigDecimal

data class DriverStanding(
    val id: Int,
    val grandPrixId: Int,
    val driverId: Int,
    val points: BigDecimal,
    val position: Int,
    val positionText: String,
    val wins: Int,
)
