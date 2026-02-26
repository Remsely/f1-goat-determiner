package dev.remsely.f1goatdeterminer.datasync.jolpica.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class RaceTable(
    val season: String? = null,

    val round: String? = null,

    @param:JsonProperty("Races")
    val races: List<RaceDto> = emptyList(),
)

data class RaceDto(
    val season: String,

    val round: String,

    val url: String? = null,

    val raceName: String,

    @param:JsonProperty("Circuit")
    val circuit: CircuitDto,

    val date: String,

    val time: String? = null,

    @param:JsonProperty("Results")
    val results: List<ResultDto>? = null,

    @param:JsonProperty("QualifyingResults")
    val qualifyingResults: List<QualifyingResultDto>? = null,
)

data class ResultDto(
    val number: String? = null,

    val position: String? = null,

    val positionText: String,

    val points: String,

    @param:JsonProperty("Driver")
    val driver: DriverDto,

    @param:JsonProperty("Constructor")
    val constructor: ConstructorDto,

    val grid: String,

    val laps: String,

    val status: String,

    @param:JsonProperty("Time")
    val time: ResultTimeDto? = null,

    @param:JsonProperty("FastestLap")
    val fastestLap: FastestLapDto? = null,
)

data class ResultTimeDto(
    val millis: String? = null,
    val time: String? = null,
)

data class FastestLapDto(
    val rank: String? = null,

    val lap: String? = null,

    @param:JsonProperty("Time")
    val time: FastestLapTimeDto? = null,

    @param:JsonProperty("AverageSpeed")
    val averageSpeed: AverageSpeedDto? = null,
)

data class FastestLapTimeDto(
    val time: String? = null,
)

data class AverageSpeedDto(
    val units: String? = null,
    val speed: String? = null,
)

data class QualifyingResultDto(
    val number: String? = null,

    val position: String,

    @param:JsonProperty("Driver")
    val driver: DriverDto,

    @param:JsonProperty("Constructor")
    val constructor: ConstructorDto,

    @param:JsonProperty("Q1")
    val q1: String? = null,

    @param:JsonProperty("Q2")
    val q2: String? = null,

    @param:JsonProperty("Q3")
    val q3: String? = null,
)
