package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.mapper.toFetchedQualifyingResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1QualifyingFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedQualifyingResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PaginationSummary
import org.springframework.stereotype.Component

@Component
class JolpicaQualifyingFetcher(private val client: JolpicaApiClient) : F1QualifyingFetcher {
    override fun forEachPageOfQualifying(
        startOffset: Int,
        onPage: (PageFetchResult<FetchedQualifyingResult>) -> Unit,
    ): PaginationSummary {
        val apiCalls = client.forEachPageOfQualifying(startOffset) { races, pageNumber, totalPages, nextOffset ->
            val items = races.flatMap { race ->
                val s = race.season.toInt()
                val r = race.round.toInt()
                race.qualifyingResults.orEmpty().map { it.toFetchedQualifyingResult(s, r) }
            }
            onPage(PageFetchResult(items, pageNumber, totalPages, nextOffset))
        }
        return PaginationSummary(apiCalls)
    }
}
