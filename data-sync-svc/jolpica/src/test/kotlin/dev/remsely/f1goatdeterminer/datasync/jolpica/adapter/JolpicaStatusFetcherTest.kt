package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.domain.result.status.Status
import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.StatusDto
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class JolpicaStatusFetcherTest {

    private val client = mockk<JolpicaApiClient>()
    private val fetcher = JolpicaStatusFetcher(client)

    @Test
    fun `maps status DTOs to domain objects`() {
        every { client.forEachPageOfStatuses(0, any()) } answers {
            val callback = secondArg<(List<StatusDto>, Int, Int, Int) -> Unit>()
            callback(
                listOf(StatusDto(statusId = "1", status = "Finished", count = "100")),
                1,
                1,
                100,
            )
            1
        }

        val pages = mutableListOf<PageFetchResult<Status>>()
        val summary = fetcher.forEachPageOfStatuses(0) { pages.add(it) }

        summary.apiCalls shouldBe 1
        pages shouldHaveSize 1
        pages[0].items[0].id shouldBe 1
        pages[0].items[0].status shouldBe "Finished"
    }

    @Test
    fun `returns api calls from client`() {
        every { client.forEachPageOfStatuses(0, any()) } returns 4

        val summary = fetcher.forEachPageOfStatuses(0) { }

        summary.apiCalls shouldBe 4
    }
}
