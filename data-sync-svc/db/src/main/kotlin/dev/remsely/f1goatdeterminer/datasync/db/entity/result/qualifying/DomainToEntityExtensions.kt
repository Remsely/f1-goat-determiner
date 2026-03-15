package dev.remsely.f1goatdeterminer.datasync.db.entity.result.qualifying

import dev.remsely.f1goatdeterminer.datasync.domain.result.qualifying.QualifyingResult

fun QualifyingResult.toEntity() = QualifyingResultEntity(
    id = id,
    raceId = grandPrixId,
    driverId = driverId,
    constructorId = constructorId,
    number = number,
    position = position,
    q1 = q1,
    q2 = q2,
    q3 = q3,
)

fun List<QualifyingResult>.toEntity(): List<QualifyingResultEntity> = map { it.toEntity() }
