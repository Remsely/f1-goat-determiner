package dev.remsely.f1goatdeterminer.datasync.jolpica.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class ConstructorTable(
    @param:JsonProperty("Constructors")
    val constructors: List<ConstructorDto> = emptyList(),
)

data class ConstructorDto(
    val constructorId: String,
    val url: String? = null,
    val name: String,
    val nationality: String? = null,
)
