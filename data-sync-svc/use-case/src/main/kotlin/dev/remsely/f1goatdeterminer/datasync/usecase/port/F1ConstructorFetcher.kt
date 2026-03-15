package dev.remsely.f1goatdeterminer.datasync.usecase.port

import dev.remsely.f1goatdeterminer.datasync.domain.constructor.Constructor

/**
 * Port for fetching F1 constructors from an external data source.
 */
interface F1ConstructorFetcher {
    fun forEachPageOfConstructors(
        startOffset: Int = 0,
        onPage: (PageFetchResult<Constructor>) -> Unit,
    ): PaginationSummary
}
