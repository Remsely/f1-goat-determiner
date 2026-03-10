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
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.RaceDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.RaceTable
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.ResultDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.StandingsListDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.StandingsTable
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.StatusDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.StatusTable
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.RateLimitExhaustedException
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatusCode
import org.springframework.web.client.HttpClientErrorException
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
        rateLimitRps = 10.0,
        retryMaxAttempts = 1,
        retryWaitDuration = Duration.ofMillis(100),
        retryBackoffMultiplier = 1.0,
    )
    private val client = JolpicaApiClient(api, retry, properties)

    @Test
    fun `forEachPageOfStatuses returns all statuses from single page`() {
        every { api.fetchStatuses(2, 0) } returns jolpicaResponse(
            total = "2",
            statusTable = StatusTable(
                listOf(
                    StatusDto("1", status = "Finished"),
                    StatusDto("2", status = "Disqualified"),
                ),
            ),
        )

        val allItems = mutableListOf<StatusDto>()
        client.forEachPageOfStatuses { items, _, _, _ -> allItems.addAll(items) }

        allItems shouldHaveSize 2
        allItems[0].status shouldBe "Finished"
    }

    @Test
    fun `forEachPageOfCircuits paginates across multiple pages`() {
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

        val allItems = mutableListOf<CircuitDto>()
        client.forEachPageOfCircuits { items, _, _, _ -> allItems.addAll(items) }

        allItems shouldHaveSize 3
        allItems[2].circuitId shouldBe "silverstone"

        verify(exactly = 1) { api.fetchCircuits(2, 0) }
        verify(exactly = 1) { api.fetchCircuits(2, 2) }
    }

    @Test
    fun `forEachPageOfStatuses with startOffset resumes from given offset`() {
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

        val allItems = mutableListOf<StatusDto>()
        client.forEachPageOfStatuses(startOffset = 5) { items, _, _, _ -> allItems.addAll(items) }

        allItems shouldHaveSize 2
        verify(exactly = 1) { api.fetchStatuses(2, 5) }
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

        retryingClient.forEachPageOfStatuses { _, _, _, _ -> }

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

        shouldThrow<RestClientException> {
            retryingClient.forEachPageOfStatuses { _, _, _, _ -> }
        }

        verify(exactly = 2) { api.fetchStatuses(2, 0) }
    }

    @Test
    fun `pagination breaks when API returns 0 records`() {
        every { api.fetchStatuses(2, 0) } returns jolpicaResponse(
            total = "5",
            statusTable = StatusTable(emptyList()),
        )

        val pages = mutableListOf<List<StatusDto>>()
        client.forEachPageOfStatuses { items, _, _, _ -> pages.add(items) }

        pages shouldHaveSize 0
        verify(exactly = 1) { api.fetchStatuses(2, 0) }
    }

    @Test
    fun `pagination uses page size for offset increment`() {
        every { api.fetchStatuses(2, 0) } returns jolpicaResponse(
            total = "3",
            offset = "0",
            statusTable = StatusTable(
                listOf(
                    StatusDto("1", status = "Finished"),
                    StatusDto("2", status = "Disqualified"),
                ),
            ),
        )
        every { api.fetchStatuses(2, 2) } returns jolpicaResponse(
            total = "3",
            offset = "2",
            statusTable = StatusTable(
                listOf(
                    StatusDto("3", status = "+1 Lap"),
                ),
            ),
        )

        val allItems = mutableListOf<StatusDto>()
        client.forEachPageOfStatuses { items, _, _, _ -> allItems.addAll(items) }

        allItems shouldHaveSize 3
        verify(exactly = 1) { api.fetchStatuses(2, 0) }
        verify(exactly = 1) { api.fetchStatuses(2, 2) }
    }

    @Test
    fun `forEachPageOfResults paginates by pageSize not by race count`() {
        val driver1 = DriverDto(driverId = "driver1", givenName = "Test", familyName = "Driver1")
        val driver2 = DriverDto(driverId = "driver2", givenName = "Test", familyName = "Driver2")
        val team1 = ConstructorDto(constructorId = "team1", name = "Team 1")
        val team2 = ConstructorDto(constructorId = "team2", name = "Team 2")
        val circuit = CircuitDto(circuitId = "monza", circuitName = "Monza")

        fun resultDto(
            pos: String,
            pts: String,
            driver: DriverDto,
            team: ConstructorDto,
            laps: String,
        ) = ResultDto(
            positionText = pos,
            points = pts,
            driver = driver,
            constructor = team,
            grid = pos,
            laps = laps,
            status = "Finished",
        )

        val result1 = resultDto("1", "25", driver1, team1, "50")
        val result2 = resultDto("2", "18", driver2, team2, "50")
        val result3 = resultDto("1", "25", driver1, team1, "52")
        val result4 = resultDto("2", "18", driver2, team2, "52")
        val result5 = resultDto("1", "25", driver1, team1, "53")

        fun raceDto(
            round: String,
            name: String,
            date: String,
            results: List<ResultDto>,
        ) = RaceDto(
            season = "2024",
            round = round,
            raceName = name,
            circuit = circuit,
            date = date,
            results = results,
        )

        every { api.fetchAllResults(2, 0) } returns jolpicaResponse(
            total = "5",
            offset = "0",
            raceTable = RaceTable(
                races = listOf(raceDto("1", "GP1", "2024-03-02", listOf(result1, result2))),
            ),
        )
        every { api.fetchAllResults(2, 2) } returns jolpicaResponse(
            total = "5",
            offset = "2",
            raceTable = RaceTable(
                races = listOf(raceDto("2", "GP2", "2024-03-16", listOf(result3, result4))),
            ),
        )
        every { api.fetchAllResults(2, 4) } returns jolpicaResponse(
            total = "5",
            offset = "4",
            raceTable = RaceTable(
                races = listOf(raceDto("3", "GP3", "2024-03-30", listOf(result5))),
            ),
        )

        val allRaces = mutableListOf<RaceDto>()
        client.forEachPageOfResults { items, _, _, _ -> allRaces.addAll(items) }

        allRaces shouldHaveSize 3
        allRaces[0].results.orEmpty() shouldHaveSize 2
        allRaces[1].results.orEmpty() shouldHaveSize 2
        allRaces[2].results.orEmpty() shouldHaveSize 1

        verify(exactly = 1) { api.fetchAllResults(2, 0) }
        verify(exactly = 1) { api.fetchAllResults(2, 2) }
        verify(exactly = 1) { api.fetchAllResults(2, 4) }
    }

    @Test
    fun `forEachPageOfSeasonDriverStandings paginates within a season`() {
        every { api.fetchSeasonDriverStandings(2024, 2, 0) } returns jolpicaResponse(
            total = "1",
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

        val allItems = mutableListOf<StandingsListDto>()
        client.forEachPageOfSeasonDriverStandings(2024) { items, _, _, _ -> allItems.addAll(items) }

        allItems shouldHaveSize 1
        allItems[0].driverStandings.shouldNotBeNull() shouldHaveSize 1

        verify(exactly = 1) { api.fetchSeasonDriverStandings(2024, 2, 0) }
    }

    @Test
    fun `forEachPageOfSeasonConstructorStandings paginates within a season`() {
        every { api.fetchSeasonConstructorStandings(2024, 2, 0) } returns jolpicaResponse(
            total = "1",
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

        val allItems = mutableListOf<StandingsListDto>()
        client.forEachPageOfSeasonConstructorStandings(2024) { items, _, _, _ -> allItems.addAll(items) }

        allItems shouldHaveSize 1
        allItems[0].constructorStandings.shouldNotBeNull() shouldHaveSize 1

        verify(exactly = 1) { api.fetchSeasonConstructorStandings(2024, 2, 0) }
    }

    @Test
    fun `retry exhaustion on 429 throws RateLimitExhaustedException`() {
        val retryConfig = RetryConfig.custom<Any>()
            .maxAttempts(2)
            .waitDuration(Duration.ofMillis(10))
            .retryOnException { it is RestClientException }
            .build()
        val retryingClient = JolpicaApiClient(api, Retry.of("retry-test", retryConfig), properties)

        every { api.fetchStatuses(2, 0) } throws
            HttpClientErrorException.create(
                HttpStatusCode.valueOf(429),
                "Too Many Requests",
                org.springframework.http.HttpHeaders.EMPTY,
                ByteArray(0),
                null,
            )

        shouldThrow<RateLimitExhaustedException> {
            retryingClient.forEachPageOfStatuses { _, _, _, _ -> }
        }
    }

    @Test
    fun `retry exhaustion on non-429 throws original exception`() {
        val retryConfig = RetryConfig.custom<Any>()
            .maxAttempts(2)
            .waitDuration(Duration.ofMillis(10))
            .retryOnException { it is RestClientException }
            .build()
        val retryingClient = JolpicaApiClient(api, Retry.of("retry-test", retryConfig), properties)

        every { api.fetchStatuses(2, 0) } throws RestClientException("server error")

        shouldThrow<RestClientException> {
            retryingClient.forEachPageOfStatuses { _, _, _, _ -> }
        }
    }

    private fun jolpicaResponse(
        total: String = "0",
        offset: String = "0",
        statusTable: StatusTable? = null,
        circuitTable: CircuitTable? = null,
        raceTable: RaceTable? = null,
        standingsTable: StandingsTable? = null,
    ) = JolpicaResponse(
        mrData = MRData(
            total = total,
            offset = offset,
            limit = properties.pageSize.toString(),
            statusTable = statusTable,
            circuitTable = circuitTable,
            raceTable = raceTable,
            standingsTable = standingsTable,
        ),
    )
}
