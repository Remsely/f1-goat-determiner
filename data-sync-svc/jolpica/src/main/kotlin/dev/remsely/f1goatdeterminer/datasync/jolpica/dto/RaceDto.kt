package dev.remsely.f1goatdeterminer.datasync.jolpica.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class RaceTable(
    val season: String? = null,

    val round: String? = null,

    @JsonProperty("Races")
    val races: List<RaceDto> = emptyList(),
)

data class RaceDto(
    val season: String,

    val round: String,

    val url: String? = null,

    val raceName: String,

    @JsonProperty("Circuit")
    val circuit: CircuitDto,

    val date: String,

    val time: String? = null,

    @JsonProperty("Results")
    val results: List<ResultDto>? = null,

    @JsonProperty("QualifyingResults")
    val qualifyingResults: List<QualifyingResultDto>? = null,
)

data class ResultDto(
    val number: String? = null,

    val position: String? = null,

    val positionText: String,

    val points: String,

    @JsonProperty("Driver")
    val driver: DriverDto,

    @JsonProperty("Constructor")
    val constructor: ConstructorDto,

    val grid: String,

    val laps: String,

    val status: String,

    @JsonProperty("Time")
    val time: ResultTimeDto? = null,

    @JsonProperty("FastestLap")
    val fastestLap: FastestLapDto? = null,
)

data class ResultTimeDto(
    val millis: String? = null,
    val time: String? = null,
)

data class FastestLapDto(
    val rank: String? = null,

    val lap: String? = null,

    @JsonProperty("Time")
    val time: FastestLapTimeDto? = null,

    @JsonProperty("AverageSpeed")
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

    @JsonProperty("Driver")
    val driver: DriverDto,

    @JsonProperty("Constructor")
    val constructor: ConstructorDto,

    @JsonProperty("Q1")
    val q1: String? = null,

    @JsonProperty("Q2")
    val q2: String? = null,

    @JsonProperty("Q3")
    val q3: String? = null,
)
