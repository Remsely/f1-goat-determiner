package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.domain.circuit.Circuit
import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.CircuitDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.LocationDto
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class JolpicaCircuitFetcherTest {

    private val client = mockk<JolpicaApiClient>()
    private val fetcher = JolpicaCircuitFetcher(client)

    @Test
    fun `maps circuit DTOs to domain objects and forwards page metadata`() {
        every { client.forEachPageOfCircuits(0, any()) } answers {
            val callback = secondArg<(List<CircuitDto>, Int, Int, Int) -> Unit>()
            callback(
                listOf(
                    CircuitDto(
                        circuitId = "monza",
                        circuitName = "Monza",
                        location = LocationDto(locality = "Monza", country = "Italy"),
                    ),
                ),
                1,
                1,
                100,
            )
            1
        }

        val pages = mutableListOf<PageFetchResult<Circuit>>()
        val summary = fetcher.forEachPageOfCircuits(0) { pages.add(it) }

        summary.apiCalls shouldBe 1
        pages shouldHaveSize 1
        pages[0].items[0].ref shouldBe "monza"
        pages[0].items[0].name shouldBe "Monza"
        pages[0].items[0].locality shouldBe "Monza"
        pages[0].items[0].country shouldBe "Italy"
        pages[0].pageNumber shouldBe 1
        pages[0].totalPages shouldBe 1
        pages[0].nextOffset shouldBe 100
    }

    @Test
    fun `passes startOffset to client`() {
        every { client.forEachPageOfCircuits(50, any()) } returns 1

        fetcher.forEachPageOfCircuits(50) { }

        // No exception means offset was forwarded correctly
    }

    @Test
    fun `returns api calls from client`() {
        every { client.forEachPageOfCircuits(0, any()) } returns 3

        val summary = fetcher.forEachPageOfCircuits(0) { }

        summary.apiCalls shouldBe 3
    }
}
