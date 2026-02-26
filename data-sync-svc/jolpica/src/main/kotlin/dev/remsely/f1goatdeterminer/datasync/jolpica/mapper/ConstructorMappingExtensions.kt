package dev.remsely.f1goatdeterminer.datasync.jolpica.mapper

import dev.remsely.f1goatdeterminer.datasync.domain.constructor.Constructor
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.ConstructorDto

fun ConstructorDto.toDomain(id: Int): Constructor = Constructor(
    id = id,
    ref = constructorId,
    name = name,
    nationality = nationality,
)
