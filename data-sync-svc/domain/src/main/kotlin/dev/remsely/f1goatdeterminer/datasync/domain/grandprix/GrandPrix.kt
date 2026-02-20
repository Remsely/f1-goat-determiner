package dev.remsely.f1goatdeterminer.datasync.domain.grandprix

import java.time.LocalDate
import java.time.LocalTime

data class GrandPrix(
    val id: Int,
    val season: Int,
    val round: Int,
    val circuitId: Int,
    val name: String,
    val date: LocalDate,
    val time: LocalTime?
)
