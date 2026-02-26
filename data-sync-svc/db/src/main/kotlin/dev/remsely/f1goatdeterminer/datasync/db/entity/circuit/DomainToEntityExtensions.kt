package dev.remsely.f1goatdeterminer.datasync.db.entity.circuit

import dev.remsely.f1goatdeterminer.datasync.domain.circuit.Circuit

fun Circuit.toEntity() = CircuitEntity(
    id = id,
    ref = ref,
    name = name,
    locality = locality,
    country = country,
)

fun List<Circuit>.toEntity(): List<CircuitEntity> = map { it.toEntity() }
