package dev.remsely.f1goatdeterminer.datasync.jolpica.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class StatusTable(
    @param:JsonProperty("Status")
    val statuses: List<StatusDto> = emptyList(),
)

data class StatusDto(
    val statusId: String,
    val count: String? = null,
    val status: String,
)
