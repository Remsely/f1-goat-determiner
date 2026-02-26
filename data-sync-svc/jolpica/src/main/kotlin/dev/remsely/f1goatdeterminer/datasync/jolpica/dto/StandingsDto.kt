package dev.remsely.f1goatdeterminer.datasync.jolpica.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class StandingsTable(
    val season: String? = null,
    val round: String? = null,
    @param:JsonProperty("StandingsLists")
    val standingsLists: List<StandingsListDto> = emptyList(),
)

data class StandingsListDto(
    val season: String,
    val round: String,
    @param:JsonProperty("DriverStandings")
    val driverStandings: List<DriverStandingDto>? = null,
    @param:JsonProperty("ConstructorStandings")
    val constructorStandings: List<ConstructorStandingDto>? = null,
)

data class DriverStandingDto(
    val position: String,
    val positionText: String,
    val points: String,
    val wins: String,
    @param:JsonProperty("Driver")
    val driver: DriverDto,
    @param:JsonProperty("Constructors")
    val constructors: List<ConstructorDto> = emptyList(),
)

data class ConstructorStandingDto(
    val position: String,
    val positionText: String,
    val points: String,
    val wins: String,
    @param:JsonProperty("Constructor")
    val constructor: ConstructorDto,
)
