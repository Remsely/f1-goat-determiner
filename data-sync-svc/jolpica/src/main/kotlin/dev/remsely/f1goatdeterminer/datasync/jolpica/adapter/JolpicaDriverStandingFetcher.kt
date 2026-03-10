package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.mapper.toFetchedDriverStanding
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1DriverStandingFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedDriverStanding
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PaginationSummary
import org.springframework.stereotype.Component

@Component
class JolpicaDriverStandingFetcher(private val client: JolpicaApiClient) : F1DriverStandingFetcher {
    override fun forEachPageOfSeasonDriverStandings(
        season: Int,
        startOffset: Int,
        onPage: (PageFetchResult<FetchedDriverStanding>) -> Unit,
    ): PaginationSummary {
        val apiCalls = client.forEachPageOfSeasonDriverStandings(
            season,
            startOffset,
        ) { standings, pageNumber, totalPages, nextOffset ->
            val items = standings.flatMap { sl ->
                val s = sl.season.toInt()
                val r = sl.round.toInt()
                sl.driverStandings.orEmpty().map { it.toFetchedDriverStanding(s, r) }
            }
            onPage(PageFetchResult(items, pageNumber, totalPages, nextOffset))
        }
        return PaginationSummary(apiCalls)
    }
}
