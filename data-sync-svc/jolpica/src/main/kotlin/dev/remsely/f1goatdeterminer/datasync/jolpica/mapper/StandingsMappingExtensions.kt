package dev.remsely.f1goatdeterminer.datasync.jolpica.mapper

import dev.remsely.f1goatdeterminer.datasync.domain.standings.constructor.ConstructorStanding
import dev.remsely.f1goatdeterminer.datasync.domain.standings.driver.DriverStanding
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.ConstructorStandingDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.DriverStandingDto
import java.math.BigDecimal

fun DriverStandingDto.toDomain(
    id: Int,
    grandPrixId: Int,
    driverId: Int,
): DriverStanding = DriverStanding(
    id = id,
    grandPrixId = grandPrixId,
    driverId = driverId,
    points = BigDecimal(points),
    position = position.toInt(),
    positionText = positionText,
    wins = wins.toInt(),
)

fun ConstructorStandingDto.toDomain(
    id: Int,
    grandPrixId: Int,
    constructorId: Int,
): ConstructorStanding = ConstructorStanding(
    id = id,
    grandPrixId = grandPrixId,
    constructorId = constructorId,
    points = BigDecimal(points),
    position = position.toInt(),
    positionText = positionText,
    wins = wins.toInt(),
)
