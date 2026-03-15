package dev.remsely.f1goatdeterminer.datasync.usecase.port

import java.time.LocalDate
import java.time.LocalTime

/**
 * Port for fetching F1 Grand Prix (race) data from an external data source.
 */
interface F1GrandPrixFetcher {
    fun forEachPageOfRaces(
        startOffset: Int = 0,
        onPage: (PageFetchResult<FetchedGrandPrix>) -> Unit,
    ): PaginationSummary
}

/**
 * Grand Prix data fetched from external source, with circuit identified by ref (not DB ID).
 */
data class FetchedGrandPrix(
    val season: Int,
    val round: Int,
    val circuitRef: String,
    val name: String,
    val date: LocalDate,
    val time: LocalTime?,
)
