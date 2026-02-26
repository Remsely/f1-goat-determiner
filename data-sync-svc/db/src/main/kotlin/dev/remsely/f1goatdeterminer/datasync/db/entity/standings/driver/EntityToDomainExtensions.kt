package dev.remsely.f1goatdeterminer.datasync.db.entity.standings.driver

import dev.remsely.f1goatdeterminer.datasync.domain.standings.driver.DriverStanding

fun DriverStandingEntity.toDomain() = DriverStanding(
    id = id,
    grandPrixId = raceId,
    driverId = driverId,
    points = points,
    position = position,
    positionText = positionText,
    wins = wins,
)

fun List<DriverStandingEntity>.toDomain(): List<DriverStanding> = map { it.toDomain() }
