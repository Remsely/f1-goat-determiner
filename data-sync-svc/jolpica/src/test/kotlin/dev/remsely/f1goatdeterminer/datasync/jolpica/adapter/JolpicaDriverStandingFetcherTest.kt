package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.DriverDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.DriverStandingDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.StandingsListDto
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedDriverStanding
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class JolpicaDriverStandingFetcherTest {

    private val client = mockk<JolpicaApiClient>()
    private val fetcher = JolpicaDriverStandingFetcher(client)

    @Test
    fun `flatMaps standings lists and extracts season and round`() {
        every { client.forEachPageOfSeasonDriverStandings(2024, 0, any()) } answers {
            val callback = thirdArg<(List<StandingsListDto>, Int, Int, Int) -> Unit>()
            callback(
                listOf(
                    StandingsListDto(
                        season = "2024",
                        round = "1",
                        driverStandings = listOf(testDriverStandingDto()),
                    ),
                    StandingsListDto(
                        season = "2024",
                        round = "2",
                        driverStandings = listOf(testDriverStandingDto(), testDriverStandingDto()),
                    ),
                ),
                1,
                1,
                100,
            )
            1
        }

        val pages = mutableListOf<PageFetchResult<FetchedDriverStanding>>()
        val summary = fetcher.forEachPageOfSeasonDriverStandings(2024, 0) { pages.add(it) }

        summary.apiCalls shouldBe 1
        pages shouldHaveSize 1
        pages[0].items shouldHaveSize 3
        pages[0].items[0].season shouldBe 2024
        pages[0].items[0].round shouldBe 1
        pages[0].items[1].round shouldBe 2
    }

    @Test
    fun `handles standings with no driver standings`() {
        every { client.forEachPageOfSeasonDriverStandings(2024, 0, any()) } answers {
            val callback = thirdArg<(List<StandingsListDto>, Int, Int, Int) -> Unit>()
            callback(
                listOf(StandingsListDto(season = "2024", round = "1", driverStandings = null)),
                1,
                1,
                100,
            )
            1
        }

        val pages = mutableListOf<PageFetchResult<FetchedDriverStanding>>()
        fetcher.forEachPageOfSeasonDriverStandings(2024, 0) { pages.add(it) }

        pages shouldHaveSize 1
        pages[0].items shouldHaveSize 0
    }

    @Test
    fun `passes season and startOffset to client`() {
        every { client.forEachPageOfSeasonDriverStandings(2023, 50, any()) } returns 2

        val summary = fetcher.forEachPageOfSeasonDriverStandings(2023, 50) { }

        summary.apiCalls shouldBe 2
    }

    private fun testDriverStandingDto() = DriverStandingDto(
        position = "1",
        positionText = "1",
        points = "25",
        wins = "1",
        driver = DriverDto(driverId = "hamilton", givenName = "Lewis", familyName = "Hamilton"),
    )
}
