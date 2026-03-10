package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.mapper.toFetchedGrandPrix
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1GrandPrixFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedGrandPrix
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PaginationSummary
import org.springframework.stereotype.Component

@Component
class JolpicaGrandPrixFetcher(private val client: JolpicaApiClient) : F1GrandPrixFetcher {
    override fun forEachPageOfRaces(
        startOffset: Int,
        onPage: (PageFetchResult<FetchedGrandPrix>) -> Unit,
    ): PaginationSummary {
        val apiCalls = client.forEachPageOfRaces(startOffset) { items, pageNumber, totalPages, nextOffset ->
            onPage(PageFetchResult(items.map { it.toFetchedGrandPrix() }, pageNumber, totalPages, nextOffset))
        }
        return PaginationSummary(apiCalls)
    }
}
