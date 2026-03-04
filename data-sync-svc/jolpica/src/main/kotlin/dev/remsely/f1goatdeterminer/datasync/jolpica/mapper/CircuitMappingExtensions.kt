package dev.remsely.f1goatdeterminer.datasync.jolpica.mapper

import dev.remsely.f1goatdeterminer.datasync.domain.circuit.Circuit
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.CircuitDto

fun CircuitDto.toDomain(): Circuit = Circuit(
    ref = circuitId,
    name = circuitName,
    locality = location?.locality,
    country = location?.country,
)
