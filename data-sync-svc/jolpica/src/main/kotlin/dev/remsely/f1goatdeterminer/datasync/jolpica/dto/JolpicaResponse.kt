package dev.remsely.f1goatdeterminer.datasync.jolpica.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class JolpicaResponse(
    @param:JsonProperty("MRData")
    val mrData: MRData,
)

data class MRData(
    val xmlns: String? = null,
    val series: String? = null,
    val url: String? = null,
    val limit: String? = null,
    val offset: String? = null,
    val total: String? = null,

    @param:JsonProperty("StatusTable")
    val statusTable: StatusTable? = null,

    @param:JsonProperty("CircuitTable")
    val circuitTable: CircuitTable? = null,

    @param:JsonProperty("ConstructorTable")
    val constructorTable: ConstructorTable? = null,

    @param:JsonProperty("DriverTable")
    val driverTable: DriverTable? = null,

    @param:JsonProperty("RaceTable")
    val raceTable: RaceTable? = null,

    @param:JsonProperty("StandingsTable")
    val standingsTable: StandingsTable? = null,

    @param:JsonProperty("SeasonTable")
    val seasonTable: SeasonTable? = null,
) {
    val totalInt: Int get() = total?.toIntOrNull() ?: 0
    val offsetInt: Int get() = offset?.toIntOrNull() ?: 0
    val limitInt: Int get() = limit?.toIntOrNull() ?: 0
}
