package dev.remsely.f1goatdeterminer.datasync.jolpica.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class JolpicaResponse(
    @JsonProperty("MRData")
    val mrData: MRData,
)

data class MRData(
    val xmlns: String? = null,
    val series: String? = null,
    val url: String? = null,
    val limit: String? = null,
    val offset: String? = null,
    val total: String? = null,

    @JsonProperty("StatusTable")
    val statusTable: StatusTable? = null,

    @JsonProperty("CircuitTable")
    val circuitTable: CircuitTable? = null,

    @JsonProperty("ConstructorTable")
    val constructorTable: ConstructorTable? = null,

    @JsonProperty("DriverTable")
    val driverTable: DriverTable? = null,

    @JsonProperty("RaceTable")
    val raceTable: RaceTable? = null,

    @JsonProperty("StandingsTable")
    val standingsTable: StandingsTable? = null,

    @JsonProperty("SeasonTable")
    val seasonTable: SeasonTable? = null,
) {
    val totalInt: Int get() = total?.toIntOrNull() ?: 0
    val offsetInt: Int get() = offset?.toIntOrNull() ?: 0
    val limitInt: Int get() = limit?.toIntOrNull() ?: 0
}
