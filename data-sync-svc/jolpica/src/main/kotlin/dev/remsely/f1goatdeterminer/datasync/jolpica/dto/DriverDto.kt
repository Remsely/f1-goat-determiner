package dev.remsely.f1goatdeterminer.datasync.jolpica.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class DriverTable(
    @param:JsonProperty("Drivers")
    val drivers: List<DriverDto> = emptyList(),
)

data class DriverDto(
    val driverId: String,
    val permanentNumber: String? = null,
    val code: String? = null,
    val url: String? = null,
    val givenName: String,
    val familyName: String,
    val dateOfBirth: String? = null,
    val nationality: String? = null,
)
