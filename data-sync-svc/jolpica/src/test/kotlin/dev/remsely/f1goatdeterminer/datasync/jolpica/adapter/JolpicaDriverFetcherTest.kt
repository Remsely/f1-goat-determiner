package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.domain.driver.Driver
import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.DriverDto
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class JolpicaDriverFetcherTest {

    private val client = mockk<JolpicaApiClient>()
    private val fetcher = JolpicaDriverFetcher(client)

    @Test
    fun `maps driver DTOs to domain objects`() {
        every { client.forEachPageOfDrivers(0, any()) } answers {
            val callback = secondArg<(List<DriverDto>, Int, Int, Int) -> Unit>()
            callback(
                listOf(
                    DriverDto(
                        driverId = "hamilton",
                        permanentNumber = "44",
                        code = "HAM",
                        givenName = "Lewis",
                        familyName = "Hamilton",
                        dateOfBirth = "1985-01-07",
                        nationality = "British",
                    ),
                ),
                1,
                1,
                100,
            )
            1
        }

        val pages = mutableListOf<PageFetchResult<Driver>>()
        val summary = fetcher.forEachPageOfDrivers(0) { pages.add(it) }

        summary.apiCalls shouldBe 1
        pages shouldHaveSize 1
        pages[0].items[0].ref shouldBe "hamilton"
        pages[0].items[0].forename shouldBe "Lewis"
        pages[0].items[0].surname shouldBe "Hamilton"
        pages[0].items[0].number shouldBe 44
        pages[0].items[0].code shouldBe "HAM"
    }

    @Test
    fun `returns api calls from client`() {
        every { client.forEachPageOfDrivers(0, any()) } returns 2

        val summary = fetcher.forEachPageOfDrivers(0) { }

        summary.apiCalls shouldBe 2
    }
}
