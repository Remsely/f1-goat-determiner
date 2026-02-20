package dev.remsely.f1goatdeterminer.datasync.db.entity.result.status

import dev.remsely.f1goatdeterminer.datasync.domain.result.status.Status

fun Status.toEntity() = StatusEntity(
    id = id,
    status = status,
)

fun List<Status>.toEntity(): List<StatusEntity> = map { it.toEntity() }
