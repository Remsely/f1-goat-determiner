package dev.remsely.f1goatdeterminer.datasync.jolpica.mapper

import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.ResultDto
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedRaceResult
import java.math.BigDecimal

fun ResultDto.toFetchedRaceResult(season: Int, round: Int): FetchedRaceResult = FetchedRaceResult(
    season = season,
    round = round,
    driverRef = driver.driverId,
    constructorRef = constructor.constructorId,
    statusText = status,
    number = number?.toIntOrNull(),
    grid = grid.toInt(),
    position = position?.toIntOrNull(),
    positionText = positionText,
    positionOrder = position?.toIntOrNull() ?: Int.MAX_VALUE,
    points = BigDecimal(points),
    laps = laps.toInt(),
    time = time?.time,
    milliseconds = time?.millis?.toLongOrNull(),
    fastestLap = fastestLap?.lap?.toIntOrNull(),
    fastestLapRank = fastestLap?.rank?.toIntOrNull(),
    fastestLapTime = fastestLap?.time?.time,
    fastestLapSpeed = fastestLap?.averageSpeed?.speed?.let { BigDecimal(it) },
)
