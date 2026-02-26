package dev.remsely.f1goatdeterminer.datasync.db.entity.constructor

import dev.remsely.f1goatdeterminer.datasync.domain.constructor.Constructor

fun ConstructorEntity.toDomain() = Constructor(
    id = id,
    ref = ref,
    name = name,
    nationality = nationality,
)

fun List<ConstructorEntity>.toDomain(): List<Constructor> = map { it.toDomain() }
