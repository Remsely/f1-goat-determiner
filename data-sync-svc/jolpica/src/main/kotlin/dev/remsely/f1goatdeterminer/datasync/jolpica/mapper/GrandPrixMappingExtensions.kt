package dev.remsely.f1goatdeterminer.datasync.jolpica.mapper

import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.RaceDto
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedGrandPrix
import java.time.LocalDate
import java.time.LocalTime

fun RaceDto.toFetchedGrandPrix(): FetchedGrandPrix = FetchedGrandPrix(
    season = season.toInt(),
    round = round.toInt(),
    circuitRef = circuit.circuitId,
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
