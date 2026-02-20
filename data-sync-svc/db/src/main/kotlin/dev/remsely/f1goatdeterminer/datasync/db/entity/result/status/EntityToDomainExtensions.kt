package dev.remsely.f1goatdeterminer.datasync.db.entity.result.status

import dev.remsely.f1goatdeterminer.datasync.domain.result.status.Status

fun StatusEntity.toDomain() = Status(
    id = id,
    status = status,
)

fun List<StatusEntity>.toDomain(): List<Status> = map { it.toDomain() }
