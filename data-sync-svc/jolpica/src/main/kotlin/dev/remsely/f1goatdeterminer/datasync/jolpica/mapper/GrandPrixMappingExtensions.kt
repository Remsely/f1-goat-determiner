package dev.remsely.f1goatdeterminer.datasync.jolpica.mapper

import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrix
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.RaceDto
import java.time.LocalDate
import java.time.LocalTime

fun RaceDto.toGrandPrix(id: Int, circuitId: Int): GrandPrix = GrandPrix(
    id = id,
    season = season.toInt(),
    round = round.toInt(),
    circuitId = circuitId,
    name = raceName,
    date = LocalDate.parse(date),
    time = time?.let { parseTime(it) },
)

private fun parseTime(time: String): LocalTime? = try {
    // API returns format like "15:00:00Z"
    LocalTime.parse(time.removeSuffix("Z"))
} catch (_: Exception) {
    null
}
