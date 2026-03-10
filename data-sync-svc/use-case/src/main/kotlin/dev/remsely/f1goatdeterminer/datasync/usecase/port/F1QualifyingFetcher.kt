package dev.remsely.f1goatdeterminer.datasync.usecase.port

/**
 * Port for fetching qualifying results from an external data source.
 */
interface F1QualifyingFetcher {
    fun forEachPageOfQualifying(
        startOffset: Int = 0,
        onPage: (PageFetchResult<FetchedQualifyingResult>) -> Unit,
    ): PaginationSummary
}

/**
 * Qualifying result data fetched from external source.
 */
data class FetchedQualifyingResult(
    val season: Int,
    val round: Int,
    val driverRef: String,
    val constructorRef: String,
    val number: Int?,
    val position: Int,
    val q1: String?,
    val q2: String?,
    val q3: String?,
)
