package dev.remsely.f1goatdeterminer.datasync.domain.standings.constructor

import java.math.BigDecimal

data class ConstructorStanding(
    val id: Int,
    val grandPrixId: Int,
    val constructorId: Int,
    val points: BigDecimal,
    val position: Int,
    val positionText: String,
    val wins: Int,
)
