package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.domain.constructor.Constructor
import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.mapper.toDomain
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1ConstructorFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PaginationSummary
import org.springframework.stereotype.Component

@Component
class JolpicaConstructorFetcher(private val client: JolpicaApiClient) : F1ConstructorFetcher {
    override fun forEachPageOfConstructors(
        startOffset: Int,
        onPage: (PageFetchResult<Constructor>) -> Unit,
    ): PaginationSummary {
        val apiCalls = client.forEachPageOfConstructors(startOffset) { dtos, pageNumber, totalPages, nextOffset ->
            onPage(PageFetchResult(dtos.map { it.toDomain() }, pageNumber, totalPages, nextOffset))
        }
        return PaginationSummary(apiCalls)
    }
}
