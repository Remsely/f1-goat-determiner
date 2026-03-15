package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.domain.constructor.Constructor
import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.ConstructorDto
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class JolpicaConstructorFetcherTest {

    private val client = mockk<JolpicaApiClient>()
    private val fetcher = JolpicaConstructorFetcher(client)

    @Test
    fun `maps constructor DTOs to domain objects`() {
        every { client.forEachPageOfConstructors(0, any()) } answers {
            val callback = secondArg<(List<ConstructorDto>, Int, Int, Int) -> Unit>()
            callback(
                listOf(ConstructorDto(constructorId = "ferrari", name = "Ferrari", nationality = "Italian")),
                1,
                1,
                100,
            )
            1
        }

        val pages = mutableListOf<PageFetchResult<Constructor>>()
        val summary = fetcher.forEachPageOfConstructors(0) { pages.add(it) }

        summary.apiCalls shouldBe 1
        pages shouldHaveSize 1
        pages[0].items[0].ref shouldBe "ferrari"
        pages[0].items[0].name shouldBe "Ferrari"
        pages[0].items[0].nationality shouldBe "Italian"
    }

    @Test
    fun `returns api calls from client`() {
        every { client.forEachPageOfConstructors(0, any()) } returns 5

        val summary = fetcher.forEachPageOfConstructors(0) { }

        summary.apiCalls shouldBe 5
    }
}
