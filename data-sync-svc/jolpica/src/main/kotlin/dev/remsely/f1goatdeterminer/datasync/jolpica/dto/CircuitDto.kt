package dev.remsely.f1goatdeterminer.datasync.jolpica.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class CircuitTable(
    @param:JsonProperty("Circuits")
    val circuits: List<CircuitDto> = emptyList(),
)

data class CircuitDto(
    val circuitId: String,

    val url: String? = null,

    val circuitName: String,

    @param:JsonProperty("Location")
    val location: LocationDto? = null,
)

data class LocationDto(
    val lat: String? = null,
    val long: String? = null,
    val locality: String? = null,
    val country: String? = null,
)
