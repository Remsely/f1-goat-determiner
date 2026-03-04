package dev.remsely.f1goatdeterminer.datasync.usecase.port

/**
 * Port for fetching the list of available F1 seasons from an external data source.
 */
interface F1SeasonFetcher {
    fun fetchAllSeasons(): List<Int>
}
