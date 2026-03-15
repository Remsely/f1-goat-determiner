package dev.remsely.f1goatdeterminer.datasync.jolpica.mapper

import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.ConstructorStandingDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.DriverStandingDto
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedConstructorStanding
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedDriverStanding
import java.math.BigDecimal

fun DriverStandingDto.toFetchedDriverStanding(season: Int, round: Int): FetchedDriverStanding =
    FetchedDriverStanding(
        season = season,
        round = round,
        driverRef = driver.driverId,
        points = BigDecimal(points),
        position = position?.toInt(),
        positionText = positionText,
        wins = wins.toInt(),
    )

fun ConstructorStandingDto.toFetchedConstructorStanding(season: Int, round: Int): FetchedConstructorStanding =
    FetchedConstructorStanding(
        season = season,
        round = round,
        constructorRef = constructor.constructorId,
        points = BigDecimal(points),
        position = position?.toInt(),
        positionText = positionText,
        wins = wins.toInt(),
    )
