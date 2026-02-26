package dev.remsely.f1goatdeterminer.datasync.jolpica.mapper

import dev.remsely.f1goatdeterminer.datasync.domain.result.race.RaceResult
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.ResultDto
import java.math.BigDecimal

fun ResultDto.toDomain(
    id: Int,
    grandPrixId: Int,
    driverId: Int,
    constructorId: Int,
    statusId: Int,
): RaceResult = RaceResult(
    id = id,
    grandPrixId = grandPrixId,
    driverId = driverId,
    constructorId = constructorId,
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
    statusId = statusId,
)
