package dev.remsely.f1goatdeterminer.datasync.app

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo

/**
 * Sets up WireMock stubs for Jolpica API to simulate a small but complete F1 dataset.
 *
 * The dataset includes:
 * - 1 status (Finished)
 * - 1 circuit (monza)
 * - 1 constructor (ferrari)
 * - 1 driver (leclerc)
 * - 1 season (2024) with 1 race (round 1)
 * - 1 race result
 * - 1 qualifying result
 * - 1 driver standing
 * - 1 constructor standing
 */
object JolpicaWireMockStubs {

    fun setupFullSyncStubs(wireMock: WireMockServer) {
        stubStatuses(wireMock)
        stubCircuits(wireMock)
        stubConstructors(wireMock)
        stubDrivers(wireMock)
        stubAllRaces(wireMock, season = 2024)
        stubAllResults(wireMock, season = 2024)
        stubAllQualifying(wireMock, season = 2024)
        stubSeasonDriverStandings(wireMock, season = 2024)
        stubSeasonConstructorStandings(wireMock, season = 2024)
    }

    fun stubStatuses(wireMock: WireMockServer, total: Int = 1) {
        wireMock.stubFor(
            get(urlPathEqualTo("/status.json"))
                .willReturn(
                    aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                        """
                        {
                            "MRData": {
                                "xmlns": "", "series": "f1", "url": "", "limit": "100", "offset": "0",
                                "total": "$total",
                                "StatusTable": {
                                    "Status": [
                                        {"statusId": "1", "count": "100", "status": "Finished"}
                                    ]
                                }
                            }
                        }
                        """,
                    ),
                ),
        )
    }

    fun stubCircuits(wireMock: WireMockServer, total: Int = 1) {
        wireMock.stubFor(
            get(urlPathEqualTo("/circuits.json"))
                .willReturn(
                    aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                        """
                        {
                            "MRData": {
                                "xmlns": "", "series": "f1", "url": "", "limit": "100", "offset": "0",
                                "total": "$total",
                                "CircuitTable": {
                                    "Circuits": [
                                        {
                                            "circuitId": "monza",
                                            "url": "http://en.wikipedia.org/wiki/Monza",
                                            "circuitName": "Autodromo Nazionale di Monza",
                                            "Location": {
                                                "lat": "45.6156", "long": "9.28111",
                                                "locality": "Monza", "country": "Italy"
                                            }
                                        }
                                    ]
                                }
                            }
                        }
                        """,
                    ),
                ),
        )
    }

    fun stubConstructors(wireMock: WireMockServer, total: Int = 1) {
        wireMock.stubFor(
            get(urlPathEqualTo("/constructors.json"))
                .willReturn(
                    aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                        """
                        {
                            "MRData": {
                                "xmlns": "", "series": "f1", "url": "", "limit": "100", "offset": "0",
                                "total": "$total",
                                "ConstructorTable": {
                                    "Constructors": [
                                        {
                                            "constructorId": "ferrari",
                                            "url": "http://en.wikipedia.org/wiki/Scuderia_Ferrari",
                                            "name": "Ferrari",
                                            "nationality": "Italian"
                                        }
                                    ]
                                }
                            }
                        }
                        """,
                    ),
                ),
        )
    }

    fun stubDrivers(wireMock: WireMockServer, total: Int = 1) {
        wireMock.stubFor(
            get(urlPathEqualTo("/drivers.json"))
                .willReturn(
                    aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                        """
                        {
                            "MRData": {
                                "xmlns": "", "series": "f1", "url": "", "limit": "100", "offset": "0",
                                "total": "$total",
                                "DriverTable": {
                                    "Drivers": [
                                        {
                                            "driverId": "leclerc",
                                            "permanentNumber": "16",
                                            "code": "LEC",
                                            "url": "http://en.wikipedia.org/wiki/Charles_Leclerc",
                                            "givenName": "Charles",
                                            "familyName": "Leclerc",
                                            "dateOfBirth": "1997-10-16",
                                            "nationality": "Monegasque"
                                        }
                                    ]
                                }
                            }
                        }
                        """,
                    ),
                ),
        )
    }

    fun stubAllRaces(wireMock: WireMockServer, season: Int) {
        wireMock.stubFor(
            get(urlPathEqualTo("/races.json"))
                .willReturn(
                    aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                        """
                        {
                            "MRData": {
                                "xmlns": "", "series": "f1", "url": "", "limit": "100", "offset": "0",
                                "total": "1",
                                "RaceTable": {
                                    "Races": [
                                        {
                                            "season": "$season", "round": "1",
                                            "url": "", "raceName": "Italian Grand Prix",
                                            "Circuit": {
                                                "circuitId": "monza", "circuitName": "Monza",
                                                "url": "",
                                                "Location": {
                                                    "lat": "45.6156", "long": "9.28111",
                                                    "locality": "Monza", "country": "Italy"
                                                }
                                            },
                                            "date": "2024-09-01", "time": "13:00:00Z"
                                        }
                                    ]
                                }
                            }
                        }
                        """,
                    ),
                ),
        )
    }

    fun stubEndpointWithError(wireMock: WireMockServer, urlPath: String, statusCode: Int = 500) {
        wireMock.stubFor(
            get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(statusCode).withBody("Internal Server Error")),
        )
    }

    fun stubAllResults(wireMock: WireMockServer, season: Int) {
        wireMock.stubFor(
            get(urlPathEqualTo("/results.json"))
                .willReturn(
                    aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                        """
                        {
                            "MRData": {
                                "xmlns": "", "series": "f1", "url": "", "limit": "100", "offset": "0",
                                "total": "1",
                                "RaceTable": {
                                    "Races": [{
                                        "season": "$season", "round": "1",
                                        "url": "", "raceName": "Italian Grand Prix",
                                        "Circuit": {
                                            "circuitId": "monza", "circuitName": "Monza", "url": "",
                                            "Location": {"lat": "0", "long": "0", "locality": "Monza", "country": "Italy"}
                                        },
                                        "date": "2024-09-01",
                                        "Results": [{
                                            "number": "16", "position": "1", "positionText": "1",
                                            "points": "25",
                                            "Driver": {
                                                "driverId": "leclerc", "permanentNumber": "16",
                                                "code": "LEC", "url": "",
                                                "givenName": "Charles", "familyName": "Leclerc",
                                                "dateOfBirth": "1997-10-16", "nationality": "Monegasque"
                                            },
                                            "Constructor": {
                                                "constructorId": "ferrari", "url": "",
                                                "name": "Ferrari", "nationality": "Italian"
                                            },
                                            "grid": "1", "laps": "53", "status": "Finished",
                                            "Time": {"millis": "4800000", "time": "1:20:00.000"},
                                            "FastestLap": {
                                                "rank": "1", "lap": "45",
                                                "Time": {"time": "1:21.500"},
                                                "AverageSpeed": {"units": "kph", "speed": "260.5"}
                                            }
                                        }]
                                    }]
                                }
                            }
                        }
                        """,
                    ),
                ),
        )
    }

    fun stubAllResultsError(wireMock: WireMockServer, statusCode: Int = 500) {
        wireMock.stubFor(
            get(urlPathEqualTo("/results.json"))
                .willReturn(aResponse().withStatus(statusCode).withBody("Internal Server Error")),
        )
    }

    fun stubAllQualifying(wireMock: WireMockServer, season: Int) {
        wireMock.stubFor(
            get(urlPathEqualTo("/qualifying.json"))
                .willReturn(
                    aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                        """
                        {
                            "MRData": {
                                "xmlns": "", "series": "f1", "url": "", "limit": "100", "offset": "0",
                                "total": "1",
                                "RaceTable": {
                                    "Races": [{
                                        "season": "$season", "round": "1",
                                        "url": "", "raceName": "Italian Grand Prix",
                                        "Circuit": {
                                            "circuitId": "monza", "circuitName": "Monza", "url": "",
                                            "Location": {"lat": "0", "long": "0", "locality": "Monza", "country": "Italy"}
                                        },
                                        "date": "2024-09-01",
                                        "QualifyingResults": [{
                                            "number": "16", "position": "1",
                                            "Driver": {
                                                "driverId": "leclerc", "permanentNumber": "16",
                                                "code": "LEC", "url": "",
                                                "givenName": "Charles", "familyName": "Leclerc",
                                                "dateOfBirth": "1997-10-16", "nationality": "Monegasque"
                                            },
                                            "Constructor": {
                                                "constructorId": "ferrari", "url": "",
                                                "name": "Ferrari", "nationality": "Italian"
                                            },
                                            "Q1": "1:20.100", "Q2": "1:19.900", "Q3": "1:19.500"
                                        }]
                                    }]
                                }
                            }
                        }
                        """,
                    ),
                ),
        )
    }

    fun stubSeasonDriverStandings(wireMock: WireMockServer, season: Int) {
        wireMock.stubFor(
            get(urlPathEqualTo("/$season/driverStandings.json"))
                .willReturn(
                    aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                        """
                        {
                            "MRData": {
                                "xmlns": "", "series": "f1", "url": "", "limit": "100", "offset": "0",
                                "total": "1",
                                "StandingsTable": {
                                    "season": "$season",
                                    "StandingsLists": [{
                                        "season": "$season", "round": "1",
                                        "DriverStandings": [{
                                            "position": "1", "positionText": "1",
                                            "points": "25", "wins": "1",
                                            "Driver": {
                                                "driverId": "leclerc", "permanentNumber": "16",
                                                "code": "LEC", "url": "",
                                                "givenName": "Charles", "familyName": "Leclerc",
                                                "dateOfBirth": "1997-10-16", "nationality": "Monegasque"
                                            },
                                            "Constructors": [{
                                                "constructorId": "ferrari", "url": "",
                                                "name": "Ferrari", "nationality": "Italian"
                                            }]
                                        }]
                                    }]
                                }
                            }
                        }
                        """,
                    ),
                ),
        )
    }

    fun stubSeasonConstructorStandings(wireMock: WireMockServer, season: Int) {
        wireMock.stubFor(
            get(urlPathEqualTo("/$season/constructorStandings.json"))
                .willReturn(
                    aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                        """
                        {
                            "MRData": {
                                "xmlns": "", "series": "f1", "url": "", "limit": "100", "offset": "0",
                                "total": "1",
                                "StandingsTable": {
                                    "season": "$season",
                                    "StandingsLists": [{
                                        "season": "$season", "round": "1",
                                        "ConstructorStandings": [{
                                            "position": "1", "positionText": "1",
                                            "points": "25", "wins": "1",
                                            "Constructor": {
                                                "constructorId": "ferrari", "url": "",
                                                "name": "Ferrari", "nationality": "Italian"
                                            }
                                        }]
                                    }]
                                }
                            }
                        }
                        """,
                    ),
                ),
        )
    }
}
