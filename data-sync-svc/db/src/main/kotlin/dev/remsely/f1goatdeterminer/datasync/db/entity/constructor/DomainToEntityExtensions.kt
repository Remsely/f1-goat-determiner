package dev.remsely.f1goatdeterminer.datasync.db.entity.constructor

import dev.remsely.f1goatdeterminer.datasync.domain.constructor.Constructor

fun Constructor.toEntity() = ConstructorEntity(
    id = id,
    ref = ref,
    name = name,
    nationality = nationality,
)

fun List<Constructor>.toEntity(): List<ConstructorEntity> = map { it.toEntity() }
