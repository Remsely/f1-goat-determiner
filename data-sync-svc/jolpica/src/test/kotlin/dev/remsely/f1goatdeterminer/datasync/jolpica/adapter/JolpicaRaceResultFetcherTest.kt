package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.CircuitDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.ConstructorDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.DriverDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.RaceDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.ResultDto
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedRaceResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class JolpicaRaceResultFetcherTest {

    private val client = mockk<JolpicaApiClient>()
    private val fetcher = JolpicaRaceResultFetcher(client)

    @Test
    fun `flatMaps race results and extracts season and round`() {
        every { client.forEachPageOfResults(0, any()) } answers {
            val callback = secondArg<(List<RaceDto>, Int, Int, Int) -> Unit>()
            callback(
                listOf(
                    testRaceDto("2024", "1", listOf(testResultDto(), testResultDto())),
                    testRaceDto("2024", "2", listOf(testResultDto())),
                ),
                1,
                1,
                100,
            )
            1
        }

        val pages = mutableListOf<PageFetchResult<FetchedRaceResult>>()
        val summary = fetcher.forEachPageOfResults(0) { pages.add(it) }

        summary.apiCalls shouldBe 1
        pages shouldHaveSize 1
        pages[0].items shouldHaveSize 3
        pages[0].items[0].season shouldBe 2024
        pages[0].items[0].round shouldBe 1
        pages[0].items[2].round shouldBe 2
    }

    @Test
    fun `handles races with no results`() {
        every { client.forEachPageOfResults(0, any()) } answers {
            val callback = secondArg<(List<RaceDto>, Int, Int, Int) -> Unit>()
            callback(listOf(testRaceDto("2024", "1", null)), 1, 1, 100)
            1
        }

        val pages = mutableListOf<PageFetchResult<FetchedRaceResult>>()
        fetcher.forEachPageOfResults(0) { pages.add(it) }

        pages shouldHaveSize 1
        pages[0].items shouldHaveSize 0
    }

    @Test
    fun `returns api calls from client`() {
        every { client.forEachPageOfResults(0, any()) } returns 5

        val summary = fetcher.forEachPageOfResults(0) { }

        summary.apiCalls shouldBe 5
    }

    private fun testRaceDto(season: String, round: String, results: List<ResultDto>?) = RaceDto(
        season = season,
        round = round,
        raceName = "GP $season-$round",
        circuit = CircuitDto(circuitId = "monza", circuitName = "Monza"),
        date = "2024-09-01",
        results = results,
    )

    private fun testResultDto() = ResultDto(
        number = "44",
        position = "1",
        positionText = "1",
        points = "25",
        driver = DriverDto(driverId = "hamilton", givenName = "Lewis", familyName = "Hamilton"),
        constructor = ConstructorDto(constructorId = "mercedes", name = "Mercedes"),
        grid = "1",
        laps = "53",
        status = "Finished",
    )
}
