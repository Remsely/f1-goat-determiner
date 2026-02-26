package dev.remsely.f1goatdeterminer.datasync.db.entity.circuit

import dev.remsely.f1goatdeterminer.datasync.domain.circuit.Circuit

fun CircuitEntity.toDomain() = Circuit(
    id = id,
    ref = ref,
    name = name,
    locality = locality,
    country = country,
)

fun List<CircuitEntity>.toDomain(): List<Circuit> = map { it.toDomain() }
