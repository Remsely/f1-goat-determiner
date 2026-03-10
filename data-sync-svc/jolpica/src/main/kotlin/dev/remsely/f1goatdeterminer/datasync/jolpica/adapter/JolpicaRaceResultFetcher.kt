package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.mapper.toFetchedRaceResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1RaceResultFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedRaceResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PaginationSummary
import org.springframework.stereotype.Component

@Component
class JolpicaRaceResultFetcher(private val client: JolpicaApiClient) : F1RaceResultFetcher {
    override fun forEachPageOfResults(
        startOffset: Int,
        onPage: (PageFetchResult<FetchedRaceResult>) -> Unit,
    ): PaginationSummary {
        val apiCalls = client.forEachPageOfResults(startOffset) { races, pageNumber, totalPages, nextOffset ->
            val items = races.flatMap { race ->
                val s = race.season.toInt()
                val r = race.round.toInt()
                race.results.orEmpty().map { it.toFetchedRaceResult(s, r) }
            }
            onPage(PageFetchResult(items, pageNumber, totalPages, nextOffset))
        }
        return PaginationSummary(apiCalls)
    }
}
