package dev.remsely.f1goatdeterminer.datasync.jolpica.client

import dev.remsely.f1goatdeterminer.datasync.jolpica.api.JolpicaApi
import dev.remsely.f1goatdeterminer.datasync.jolpica.config.JolpicaClientProperties
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.CircuitDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.CircuitTable
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.ConstructorDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.ConstructorStandingDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.DriverDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.DriverStandingDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.JolpicaResponse
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.MRData
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.QualifyingResultDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.RaceDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.RaceTable
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.ResultDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.SeasonDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.SeasonTable
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.StandingsListDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.StandingsTable
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.StatusDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.StatusTable
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.client.RestClientException
import java.time.Duration

class JolpicaApiClientTest {

    private val api: JolpicaApi = mockk()
    private val retry: Retry = Retry.of("test", RetryConfig.ofDefaults())
    private val properties = JolpicaClientProperties(
        baseUrl = "http://localhost",
        connectTimeout = Duration.ofSeconds(5),
        readTimeout = Duration.ofSeconds(5),
        pageSize = 2,
        rateLimit = 10,
        retryMaxAttempts = 1,
        retryWaitDuration = Duration.ofMillis(100),
    )
    private val client = JolpicaApiClient(api, retry, properties)

    @Test
    fun `fetchAllStatuses returns all statuses from single page`() {
        every { api.fetchStatuses(2, 0) } returns jolpicaResponse(
            total = "2",
            statusTable = StatusTable(
                listOf(
                    StatusDto("1", status = "Finished"),
                    StatusDto("2", status = "Disqualified"),
                ),
            ),
        )

        val result = client.fetchAllStatuses()

        result shouldHaveSize 2
        result[0].status shouldBe "Finished"
    }

    @Test
    fun `fetchAllCircuits paginates across multiple pages`() {
        every { api.fetchCircuits(2, 0) } returns jolpicaResponse(
            total = "3",
            offset = "0",
            circuitTable = CircuitTable(
                listOf(
                    CircuitDto("monza", circuitName = "Monza"),
                    CircuitDto("spa", circuitName = "Spa"),
                ),
            ),
        )
        every { api.fetchCircuits(2, 2) } returns jolpicaResponse(
            total = "3",
            offset = "2",
            circuitTable = CircuitTable(
                listOf(
                    CircuitDto("silverstone", circuitName = "Silverstone"),
                ),
            ),
        )

        val result = client.fetchAllCircuits()

        result shouldHaveSize 3
        result[2].circuitId shouldBe "silverstone"

        verify(exactly = 1) { api.fetchCircuits(2, 0) }
        verify(exactly = 1) { api.fetchCircuits(2, 2) }
    }

    @Test
    fun `fetchAllStatuses with startOffset resumes from given offset`() {
        every { api.fetchStatuses(2, 5) } returns jolpicaResponse(
            total = "7",
            offset = "5",
            statusTable = StatusTable(
                listOf(
                    StatusDto("6", status = "Engine"),
                    StatusDto("7", status = "Gearbox"),
                ),
            ),
        )

        val result = client.fetchAllStatuses(startOffset = 5)

        result shouldHaveSize 2
        verify(exactly = 1) { api.fetchStatuses(2, 5) }
    }

    @Test
    fun `fetchAllSeasons returns parsed season years`() {
        every { api.fetchSeasons(2, 0) } returns jolpicaResponse(
            total = "2",
            seasonTable = SeasonTable(
                listOf(
                    SeasonDto("1950"),
                    SeasonDto("2024"),
                ),
            ),
        )

        val result = client.fetchAllSeasons()

        result shouldBe listOf(1950, 2024)
    }

    @Test
    fun `fetchResults returns race results for given season and round`() {
        val resultDto = ResultDto(
            number = "1",
            position = "1",
            positionText = "1",
            points = "26",
            driver = DriverDto(driverId = "max_verstappen", givenName = "Max", familyName = "Verstappen"),
            constructor = ConstructorDto(constructorId = "red_bull", name = "Red Bull"),
            grid = "1",
            laps = "57",
            status = "Finished",
        )
        every { api.fetchResults(2024, 1) } returns jolpicaResponse(
            raceTable = RaceTable(
                season = "2024",
                round = "1",
                races = listOf(
                    RaceDto(
                        season = "2024",
                        round = "1",
                        raceName = "Bahrain GP",
                        circuit = CircuitDto(circuitId = "bahrain", circuitName = "Bahrain"),
                        date = "2024-03-02",
                        results = listOf(resultDto),
                    ),
                ),
            ),
        )

        val result = client.fetchResults(2024, 1)

        result shouldHaveSize 1
        result[0].results.shouldNotBeNull() shouldHaveSize 1
    }

    @Test
    fun `fetchQualifying returns qualifying results for given season and round`() {
        val qualifyingDto = QualifyingResultDto(
            number = "1",
            position = "1",
            driver = DriverDto(driverId = "max_verstappen", givenName = "Max", familyName = "Verstappen"),
            constructor = ConstructorDto(constructorId = "red_bull", name = "Red Bull"),
            q1 = "1:30.000",
        )
        every { api.fetchQualifying(2024, 1) } returns jolpicaResponse(
            raceTable = RaceTable(
                races = listOf(
                    RaceDto(
                        season = "2024",
                        round = "1",
                        raceName = "Bahrain GP",
                        circuit = CircuitDto(circuitId = "bahrain", circuitName = "Bahrain"),
                        date = "2024-03-02",
                        qualifyingResults = listOf(qualifyingDto),
                    ),
                ),
            ),
        )

        val result = client.fetchQualifying(2024, 1)

        result shouldHaveSize 1
        result[0].qualifyingResults.shouldNotBeNull() shouldHaveSize 1
    }

    @Test
    fun `fetchDriverStandings returns standings for given season and round`() {
        every { api.fetchDriverStandings(2024, 1) } returns jolpicaResponse(
            standingsTable = StandingsTable(
                standingsLists = listOf(
                    StandingsListDto(
                        season = "2024",
                        round = "1",
                        driverStandings = listOf(
                            DriverStandingDto(
                                position = "1",
                                positionText = "1",
                                points = "26",
                                wins = "1",
                                driver = DriverDto(
                                    driverId = "max_verstappen",
                                    givenName = "Max",
                                    familyName = "Verstappen",
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val result = client.fetchDriverStandings(2024, 1)

        result shouldHaveSize 1
        result[0].driverStandings.shouldNotBeNull() shouldHaveSize 1
    }

    @Test
    fun `fetchConstructorStandings returns standings for given season and round`() {
        every { api.fetchConstructorStandings(2024, 1) } returns jolpicaResponse(
            standingsTable = StandingsTable(
                standingsLists = listOf(
                    StandingsListDto(
                        season = "2024",
                        round = "1",
                        constructorStandings = listOf(
                            ConstructorStandingDto(
                                position = "1",
                                positionText = "1",
                                points = "44",
                                wins = "1",
                                constructor = ConstructorDto(constructorId = "red_bull", name = "Red Bull"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val result = client.fetchConstructorStandings(2024, 1)

        result shouldHaveSize 1
        result[0].constructorStandings.shouldNotBeNull() shouldHaveSize 1
    }

    @Test
    fun `retry retries on RestClientException and eventually succeeds`() {
        val retryConfig = RetryConfig.custom<Any>()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(10))
            .retryOnException { it is RestClientException }
            .build()
        val retryingClient = JolpicaApiClient(api, Retry.of("retry-test", retryConfig), properties)

        every { api.fetchStatuses(2, 0) } throws RestClientException("timeout") andThen
            jolpicaResponse(
                total = "1",
                statusTable = StatusTable(listOf(StatusDto("1", status = "Finished"))),
            )

        val result = retryingClient.fetchAllStatuses()

        result shouldHaveSize 1
        verify(exactly = 2) { api.fetchStatuses(2, 0) }
    }

    @Test
    fun `retry exhausts attempts and throws on persistent failure`() {
        val retryConfig = RetryConfig.custom<Any>()
            .maxAttempts(2)
            .waitDuration(Duration.ofMillis(10))
            .retryOnException { it is RestClientException }
            .build()
        val retryingClient = JolpicaApiClient(api, Retry.of("retry-test", retryConfig), properties)

        every { api.fetchStatuses(2, 0) } throws RestClientException("persistent failure")

        assertThrows<RestClientException> {
            retryingClient.fetchAllStatuses()
        }

        verify(exactly = 2) { api.fetchStatuses(2, 0) }
    }

    private fun jolpicaResponse(
        total: String = "0",
        offset: String = "0",
        statusTable: StatusTable? = null,
        circuitTable: CircuitTable? = null,
        seasonTable: SeasonTable? = null,
        raceTable: RaceTable? = null,
        standingsTable: StandingsTable? = null,
    ) = JolpicaResponse(
        mrData = MRData(
            total = total,
            offset = offset,
            limit = properties.pageSize.toString(),
            statusTable = statusTable,
            circuitTable = circuitTable,
            seasonTable = seasonTable,
            raceTable = raceTable,
            standingsTable = standingsTable,
        ),
    )
}
