package dev.remsely.f1goatdeterminer.datasync.db.entity.standings.driver

import dev.remsely.f1goatdeterminer.datasync.domain.standings.driver.DriverStanding

fun DriverStanding.toEntity() = DriverStandingEntity(
    id = id,
    raceId = grandPrixId,
    driverId = driverId,
    points = points,
    position = position,
    positionText = positionText,
    wins = wins,
)

fun List<DriverStanding>.toEntity(): List<DriverStandingEntity> = map { it.toEntity() }
