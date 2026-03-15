package dev.remsely.f1goatdeterminer.datasync.jolpica.mapper

import dev.remsely.f1goatdeterminer.datasync.domain.result.status.Status
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.StatusDto

fun StatusDto.toDomain(): Status = Status(
    id = statusId.toInt(),
    status = status,
)

fun List<StatusDto>.toDomain(): List<Status> = map { it.toDomain() }
