package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.domain.driver.Driver
import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.mapper.toDomain
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1DriverFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PaginationSummary
import org.springframework.stereotype.Component

@Component
class JolpicaDriverFetcher(private val client: JolpicaApiClient) : F1DriverFetcher {
    override fun forEachPageOfDrivers(
        startOffset: Int,
        onPage: (PageFetchResult<Driver>) -> Unit,
    ): PaginationSummary {
        val apiCalls = client.forEachPageOfDrivers(startOffset) { dtos, pageNumber, totalPages, nextOffset ->
            onPage(PageFetchResult(dtos.map { it.toDomain() }, pageNumber, totalPages, nextOffset))
        }
        return PaginationSummary(apiCalls)
    }
}
