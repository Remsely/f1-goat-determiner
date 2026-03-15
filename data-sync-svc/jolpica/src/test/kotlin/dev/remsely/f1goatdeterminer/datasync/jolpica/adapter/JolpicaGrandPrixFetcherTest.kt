package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.CircuitDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.LocationDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.RaceDto
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedGrandPrix
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.LocalDate

class JolpicaGrandPrixFetcherTest {

    private val client = mockk<JolpicaApiClient>()
    private val fetcher = JolpicaGrandPrixFetcher(client)

    @Test
    fun `maps race DTOs to fetched grand prix objects`() {
        every { client.forEachPageOfRaces(0, any()) } answers {
            val callback = secondArg<(List<RaceDto>, Int, Int, Int) -> Unit>()
            callback(
                listOf(
                    RaceDto(
                        season = "2024",
                        round = "1",
                        raceName = "Bahrain GP",
                        circuit = CircuitDto(
                            circuitId = "bahrain",
                            circuitName = "Bahrain International Circuit",
                            location = LocationDto(locality = "Sakhir", country = "Bahrain"),
                        ),
                        date = "2024-03-02",
                        time = "15:00:00Z",
                    ),
                ),
                1,
                1,
                100,
            )
            1
        }

        val pages = mutableListOf<PageFetchResult<FetchedGrandPrix>>()
        val summary = fetcher.forEachPageOfRaces(0) { pages.add(it) }

        summary.apiCalls shouldBe 1
        pages shouldHaveSize 1
        pages[0].items[0].season shouldBe 2024
        pages[0].items[0].round shouldBe 1
        pages[0].items[0].circuitRef shouldBe "bahrain"
        pages[0].items[0].name shouldBe "Bahrain GP"
        pages[0].items[0].date shouldBe LocalDate.of(2024, 3, 2)
    }

    @Test
    fun `returns api calls from client`() {
        every { client.forEachPageOfRaces(0, any()) } returns 3

        val summary = fetcher.forEachPageOfRaces(0) { }

        summary.apiCalls shouldBe 3
    }
}
