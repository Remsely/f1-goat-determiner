package dev.remsely.f1goatdeterminer.datasync.db.entity.result.race

import dev.remsely.f1goatdeterminer.datasync.domain.result.race.RaceResult

fun RaceResultEntity.toDomain() = RaceResult(
    id = id,
    grandPrixId = raceId,
    driverId = driverId,
    constructorId = constructorId,
    number = number,
    grid = grid,
    position = position,
    positionText = positionText,
    positionOrder = positionOrder,
    points = points,
    laps = laps,
    time = time,
    milliseconds = milliseconds,
    fastestLap = fastestLap,
    fastestLapRank = fastestLapRank,
    fastestLapTime = fastestLapTime,
    fastestLapSpeed = fastestLapSpeed,
    statusId = statusId,
)

fun List<RaceResultEntity>.toDomain(): List<RaceResult> = map { it.toDomain() }
