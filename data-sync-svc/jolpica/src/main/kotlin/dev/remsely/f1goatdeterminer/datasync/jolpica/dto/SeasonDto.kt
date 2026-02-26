package dev.remsely.f1goatdeterminer.datasync.jolpica.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class SeasonTable(
    @param:JsonProperty("Seasons")
    val seasons: List<SeasonDto> = emptyList(),
)

data class SeasonDto(
    val season: String,
    val url: String? = null,
)
