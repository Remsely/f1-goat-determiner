package dev.remsely.f1goatdeterminer.datasync.jolpica.dto

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class JolpicaResponseDeserializationTest {

    private val objectMapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    @Test
    fun `deserialize status response`() {
        val json = """
        {
            "MRData": {
                "xmlns": "",
                "series": "f1",
                "limit": "3",
                "offset": "0",
                "total": "136",
                "StatusTable": {
                    "Status": [
                        {"statusId": "1", "count": "8004", "status": "Finished"},
                        {"statusId": "11", "count": "3850", "status": "+1 Lap"}
                    ]
                }
            }
        }
        """.trimIndent()

        val response = objectMapper.readValue<JolpicaResponse>(json)

        response.mrData.totalInt shouldBe 136
        response.mrData.offsetInt shouldBe 0

        val statuses = response.mrData.statusTable.shouldNotBeNull().statuses
        statuses shouldHaveSize 2
        statuses[0].statusId shouldBe "1"
        statuses[0].status shouldBe "Finished"
    }

    @Test
    fun `deserialize circuit response`() {
        val json = """
        {
            "MRData": {
                "limit": "2",
                "offset": "0",
                "total": "78",
                "CircuitTable": {
                    "Circuits": [
                        {
                            "circuitId": "adelaide",
                            "circuitName": "Adelaide Street Circuit",
                            "Location": {
                                "lat": "-34.9272",
                                "long": "138.617",
                                "locality": "Adelaide",
                                "country": "Australia"
                            }
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        val response = objectMapper.readValue<JolpicaResponse>(json)

        val circuits = response.mrData.circuitTable.shouldNotBeNull().circuits
        circuits shouldHaveSize 1
        circuits[0].circuitId shouldBe "adelaide"
        circuits[0].circuitName shouldBe "Adelaide Street Circuit"
        circuits[0].location.shouldNotBeNull()
        circuits[0].location?.locality shouldBe "Adelaide"
        circuits[0].location?.country shouldBe "Australia"
    }

    @Test
    fun `deserialize driver response`() {
        val json = """
        {
            "MRData": {
                "limit": "1",
                "offset": "0",
                "total": "874",
                "DriverTable": {
                    "Drivers": [
                        {
                            "driverId": "hamilton",
                            "permanentNumber": "44",
                            "code": "HAM",
                            "givenName": "Lewis",
                            "familyName": "Hamilton",
                            "dateOfBirth": "1985-01-07",
                            "nationality": "British"
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        val response = objectMapper.readValue<JolpicaResponse>(json)

        val drivers = response.mrData.driverTable.shouldNotBeNull().drivers
        drivers shouldHaveSize 1
        drivers[0].driverId shouldBe "hamilton"
        drivers[0].permanentNumber shouldBe "44"
        drivers[0].code shouldBe "HAM"
    }

    @Test
    fun `deserialize constructor response`() {
        val json = """
        {
            "MRData": {
                "limit": "1",
                "offset": "0",
                "total": "214",
                "ConstructorTable": {
                    "Constructors": [
                        {
                            "constructorId": "ferrari",
                            "name": "Ferrari",
                            "nationality": "Italian"
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        val response = objectMapper.readValue<JolpicaResponse>(json)

        val constructors = response.mrData.constructorTable.shouldNotBeNull().constructors
        constructors shouldHaveSize 1
        constructors[0].constructorId shouldBe "ferrari"
        constructors[0].name shouldBe "Ferrari"
    }

    @Test
    fun `deserialize race results response`() {
        val json = """
        {
            "MRData": {
                "limit": "30",
                "offset": "0",
                "total": "20",
                "RaceTable": {
                    "season": "2024",
                    "round": "1",
                    "Races": [
                        {
                            "season": "2024",
                            "round": "1",
                            "raceName": "Bahrain Grand Prix",
                            "Circuit": {
                                "circuitId": "bahrain",
                                "circuitName": "Bahrain International Circuit"
                            },
                            "date": "2024-03-02",
                            "time": "15:00:00Z",
                            "Results": [
                                {
                                    "number": "1",
                                    "position": "1",
                                    "positionText": "1",
                                    "points": "26",
                                    "Driver": {
                                        "driverId": "max_verstappen",
                                        "permanentNumber": "3",
                                        "code": "VER",
                                        "givenName": "Max",
                                        "familyName": "Verstappen",
                                        "dateOfBirth": "1997-09-30",
                                        "nationality": "Dutch"
                                    },
                                    "Constructor": {
                                        "constructorId": "red_bull",
                                        "name": "Red Bull",
                                        "nationality": "Austrian"
                                    },
                                    "grid": "1",
                                    "laps": "57",
                                    "status": "Finished",
                                    "Time": {
                                        "millis": "5504742",
                                        "time": "1:31:44.742"
                                    },
                                    "FastestLap": {
                                        "rank": "1",
                                        "lap": "39",
                                        "Time": {"time": "1:32.608"},
                                        "AverageSpeed": {"units": "kph", "speed": "210.383"}
                                    }
                                }
                            ]
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        val response = objectMapper.readValue<JolpicaResponse>(json)

        val races = response.mrData.raceTable.shouldNotBeNull().races
        races shouldHaveSize 1

        val race = races[0]
        race.raceName shouldBe "Bahrain Grand Prix"
        race.circuit.circuitId shouldBe "bahrain"

        val results = race.results.shouldNotBeNull()
        results shouldHaveSize 1
        results[0].positionText shouldBe "1"
        results[0].points shouldBe "26"
        results[0].driver.driverId shouldBe "max_verstappen"
        results[0].constructor.constructorId shouldBe "red_bull"
        results[0].fastestLap.shouldNotBeNull().rank shouldBe "1"
        results[0].fastestLap?.averageSpeed?.speed shouldBe "210.383"
    }

    @Test
    fun `deserialize qualifying response`() {
        val json = """
        {
            "MRData": {
                "limit": "30",
                "offset": "0",
                "total": "20",
                "RaceTable": {
                    "season": "2024",
                    "round": "1",
                    "Races": [
                        {
                            "season": "2024",
                            "round": "1",
                            "raceName": "Bahrain Grand Prix",
                            "Circuit": {
                                "circuitId": "bahrain",
                                "circuitName": "Bahrain International Circuit"
                            },
                            "date": "2024-03-02",
                            "time": "15:00:00Z",
                            "QualifyingResults": [
                                {
                                    "number": "1",
                                    "position": "1",
                                    "Driver": {
                                        "driverId": "max_verstappen",
                                        "givenName": "Max",
                                        "familyName": "Verstappen"
                                    },
                                    "Constructor": {
                                        "constructorId": "red_bull",
                                        "name": "Red Bull"
                                    },
                                    "Q1": "1:30.031",
                                    "Q2": "1:29.374",
                                    "Q3": "1:29.179"
                                }
                            ]
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        val response = objectMapper.readValue<JolpicaResponse>(json)

        val races = response.mrData.raceTable.shouldNotBeNull().races
        val qualifying = races[0].qualifyingResults.shouldNotBeNull()
        qualifying shouldHaveSize 1
        qualifying[0].position shouldBe "1"
        qualifying[0].q1 shouldBe "1:30.031"
        qualifying[0].q3 shouldBe "1:29.179"
    }

    @Test
    fun `deserialize driver standings response`() {
        val json = """
        {
            "MRData": {
                "limit": "30",
                "offset": "0",
                "total": "20",
                "StandingsTable": {
                    "season": "2024",
                    "round": "1",
                    "StandingsLists": [
                        {
                            "season": "2024",
                            "round": "1",
                            "DriverStandings": [
                                {
                                    "position": "1",
                                    "positionText": "1",
                                    "points": "26",
                                    "wins": "1",
                                    "Driver": {
                                        "driverId": "max_verstappen",
                                        "givenName": "Max",
                                        "familyName": "Verstappen"
                                    },
                                    "Constructors": [
                                        {
                                            "constructorId": "red_bull",
                                            "name": "Red Bull"
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        val response = objectMapper.readValue<JolpicaResponse>(json)

        val standings = response.mrData.standingsTable.shouldNotBeNull().standingsLists
        standings shouldHaveSize 1

        val driverStandings = standings[0].driverStandings.shouldNotBeNull()
        driverStandings shouldHaveSize 1
        driverStandings[0].position shouldBe "1"
        driverStandings[0].points shouldBe "26"
        driverStandings[0].wins shouldBe "1"
        driverStandings[0].driver.driverId shouldBe "max_verstappen"
    }

    @Test
    fun `deserialize constructor standings response`() {
        val json = """
        {
            "MRData": {
                "limit": "30",
                "offset": "0",
                "total": "10",
                "StandingsTable": {
                    "season": "2024",
                    "round": "1",
                    "StandingsLists": [
                        {
                            "season": "2024",
                            "round": "1",
                            "ConstructorStandings": [
                                {
                                    "position": "1",
                                    "positionText": "1",
                                    "points": "44",
                                    "wins": "1",
                                    "Constructor": {
                                        "constructorId": "red_bull",
                                        "name": "Red Bull",
                                        "nationality": "Austrian"
                                    }
                                }
                            ]
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        val response = objectMapper.readValue<JolpicaResponse>(json)

        val standings = response.mrData.standingsTable.shouldNotBeNull().standingsLists
        val constructorStandings = standings[0].constructorStandings.shouldNotBeNull()
        constructorStandings shouldHaveSize 1
        constructorStandings[0].position shouldBe "1"
        constructorStandings[0].points shouldBe "44"
        constructorStandings[0].constructor.constructorId shouldBe "red_bull"
    }

    @Test
    fun `deserialize season response`() {
        val json = """
        {
            "MRData": {
                "limit": "100",
                "offset": "0",
                "total": "77",
                "SeasonTable": {
                    "Seasons": [
                        {"season": "1950"},
                        {"season": "2024"}
                    ]
                }
            }
        }
        """.trimIndent()

        val response = objectMapper.readValue<JolpicaResponse>(json)

        val seasons = response.mrData.seasonTable.shouldNotBeNull().seasons
        seasons shouldHaveSize 2
        seasons[0].season shouldBe "1950"
        seasons[1].season shouldBe "2024"
    }
}
