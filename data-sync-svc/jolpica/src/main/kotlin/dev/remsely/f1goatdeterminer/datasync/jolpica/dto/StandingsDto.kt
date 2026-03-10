package dev.remsely.f1goatdeterminer.datasync.jolpica.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class StandingsTable(
    val season: String? = null,

    val round: String? = null,

    @JsonProperty("StandingsLists")
    val standingsLists: List<StandingsListDto> = emptyList(),
)

data class StandingsListDto(
    val season: String,

    val round: String,

    @JsonProperty("DriverStandings")
    val driverStandings: List<DriverStandingDto>? = null,

    @JsonProperty("ConstructorStandings")
    val constructorStandings: List<ConstructorStandingDto>? = null,
)

data class DriverStandingDto(
    val position: String? = null,

    val positionText: String,

    val points: String,

    val wins: String,

    @JsonProperty("Driver")
    val driver: DriverDto,

    @JsonProperty("Constructors")
    val constructors: List<ConstructorDto> = emptyList(),
)

data class ConstructorStandingDto(
    val position: String? = null,

    val positionText: String,

    val points: String,

    val wins: String,

    @JsonProperty("Constructor")
    val constructor: ConstructorDto,
)
