package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.CircuitDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.ConstructorDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.DriverDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.QualifyingResultDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.RaceDto
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedQualifyingResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class JolpicaQualifyingFetcherTest {

    private val client = mockk<JolpicaApiClient>()
    private val fetcher = JolpicaQualifyingFetcher(client)

    @Test
    fun `flatMaps qualifying results and extracts season and round`() {
        every { client.forEachPageOfQualifying(0, any()) } answers {
            val callback = secondArg<(List<RaceDto>, Int, Int, Int) -> Unit>()
            callback(
                listOf(
                    testRaceDto("2024", "1", listOf(testQualifyingDto(), testQualifyingDto())),
                    testRaceDto("2024", "2", listOf(testQualifyingDto())),
                ),
                1,
                1,
                100,
            )
            1
        }

        val pages = mutableListOf<PageFetchResult<FetchedQualifyingResult>>()
        val summary = fetcher.forEachPageOfQualifying(0) { pages.add(it) }

        summary.apiCalls shouldBe 1
        pages shouldHaveSize 1
        pages[0].items shouldHaveSize 3
        pages[0].items[0].season shouldBe 2024
        pages[0].items[0].round shouldBe 1
        pages[0].items[2].round shouldBe 2
    }

    @Test
    fun `handles races with no qualifying results`() {
        every { client.forEachPageOfQualifying(0, any()) } answers {
            val callback = secondArg<(List<RaceDto>, Int, Int, Int) -> Unit>()
            callback(listOf(testRaceDto("2024", "1", null)), 1, 1, 100)
            1
        }

        val pages = mutableListOf<PageFetchResult<FetchedQualifyingResult>>()
        fetcher.forEachPageOfQualifying(0) { pages.add(it) }

        pages shouldHaveSize 1
        pages[0].items shouldHaveSize 0
    }

    @Test
    fun `returns api calls from client`() {
        every { client.forEachPageOfQualifying(0, any()) } returns 2

        val summary = fetcher.forEachPageOfQualifying(0) { }

        summary.apiCalls shouldBe 2
    }

    private fun testRaceDto(
        season: String,
        round: String,
        qualifyingResults: List<QualifyingResultDto>?,
    ) = RaceDto(
        season = season,
        round = round,
        raceName = "GP $season-$round",
        circuit = CircuitDto(circuitId = "monza", circuitName = "Monza"),
        date = "2024-09-01",
        qualifyingResults = qualifyingResults,
    )

    private fun testQualifyingDto() = QualifyingResultDto(
        number = "44",
        position = "1",
        driver = DriverDto(driverId = "hamilton", givenName = "Lewis", familyName = "Hamilton"),
        constructor = ConstructorDto(constructorId = "mercedes", name = "Mercedes"),
        q1 = "1:23.456",
        q2 = "1:22.345",
        q3 = "1:21.234",
    )
}
