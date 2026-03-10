package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.mapper.toFetchedConstructorStanding
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1ConstructorStandingFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedConstructorStanding
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PaginationSummary
import org.springframework.stereotype.Component

@Component
class JolpicaConstructorStandingFetcher(private val client: JolpicaApiClient) : F1ConstructorStandingFetcher {
    override fun forEachPageOfSeasonConstructorStandings(
        season: Int,
        startOffset: Int,
        onPage: (PageFetchResult<FetchedConstructorStanding>) -> Unit,
    ): PaginationSummary {
        val apiCalls = client.forEachPageOfSeasonConstructorStandings(
            season,
            startOffset,
        ) { standings, pageNumber, totalPages, nextOffset ->
            val items = standings.flatMap { sl ->
                val s = sl.season.toInt()
                val r = sl.round.toInt()
                sl.constructorStandings.orEmpty().map { it.toFetchedConstructorStanding(s, r) }
            }
            onPage(PageFetchResult(items, pageNumber, totalPages, nextOffset))
        }
        return PaginationSummary(apiCalls)
    }
}
