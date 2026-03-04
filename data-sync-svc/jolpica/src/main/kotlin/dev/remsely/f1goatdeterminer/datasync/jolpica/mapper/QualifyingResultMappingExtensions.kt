package dev.remsely.f1goatdeterminer.datasync.jolpica.mapper

import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.QualifyingResultDto
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedQualifyingResult

fun QualifyingResultDto.toFetchedQualifyingResult(season: Int, round: Int): FetchedQualifyingResult =
    FetchedQualifyingResult(
        season = season,
        round = round,
        driverRef = driver.driverId,
        constructorRef = constructor.constructorId,
        number = number?.toIntOrNull(),
        position = position.toInt(),
        q1 = q1,
        q2 = q2,
        q3 = q3,
    )
