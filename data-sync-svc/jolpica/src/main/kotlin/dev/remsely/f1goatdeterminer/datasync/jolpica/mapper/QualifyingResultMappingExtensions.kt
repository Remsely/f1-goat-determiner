package dev.remsely.f1goatdeterminer.datasync.jolpica.mapper

import dev.remsely.f1goatdeterminer.datasync.domain.result.qualifying.QualifyingResult
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.QualifyingResultDto

fun QualifyingResultDto.toDomain(
    id: Int,
    grandPrixId: Int,
    driverId: Int,
    constructorId: Int,
): QualifyingResult = QualifyingResult(
    id = id,
    grandPrixId = grandPrixId,
    driverId = driverId,
    constructorId = constructorId,
    number = number?.toIntOrNull(),
    position = position.toInt(),
    q1 = q1,
    q2 = q2,
    q3 = q3,
)
