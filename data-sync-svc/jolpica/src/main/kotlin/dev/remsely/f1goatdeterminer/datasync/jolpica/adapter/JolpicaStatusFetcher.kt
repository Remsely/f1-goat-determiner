package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.domain.result.status.Status
import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.mapper.toDomain
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1StatusFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PaginationSummary
import org.springframework.stereotype.Component

@Component
class JolpicaStatusFetcher(private val client: JolpicaApiClient) : F1StatusFetcher {
    override fun forEachPageOfStatuses(
        startOffset: Int,
        onPage: (PageFetchResult<Status>) -> Unit,
    ): PaginationSummary {
        val apiCalls = client.forEachPageOfStatuses(startOffset) { dtos, pageNumber, totalPages, nextOffset ->
            onPage(PageFetchResult(dtos.map { it.toDomain() }, pageNumber, totalPages, nextOffset))
        }
        return PaginationSummary(apiCalls)
    }
}
