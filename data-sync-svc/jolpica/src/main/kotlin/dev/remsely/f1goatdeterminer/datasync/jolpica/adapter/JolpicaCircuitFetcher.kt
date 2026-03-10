package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.domain.circuit.Circuit
import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.mapper.toDomain
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1CircuitFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PaginationSummary
import org.springframework.stereotype.Component

@Component
class JolpicaCircuitFetcher(private val client: JolpicaApiClient) : F1CircuitFetcher {
    override fun forEachPageOfCircuits(
        startOffset: Int,
        onPage: (PageFetchResult<Circuit>) -> Unit,
    ): PaginationSummary {
        val apiCalls = client.forEachPageOfCircuits(startOffset) { dtos, pageNumber, totalPages, nextOffset ->
            onPage(PageFetchResult(dtos.map { it.toDomain() }, pageNumber, totalPages, nextOffset))
        }
        return PaginationSummary(apiCalls)
    }
}
