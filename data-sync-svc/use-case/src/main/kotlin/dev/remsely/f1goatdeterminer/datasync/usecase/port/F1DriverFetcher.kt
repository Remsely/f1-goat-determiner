package dev.remsely.f1goatdeterminer.datasync.usecase.port

import dev.remsely.f1goatdeterminer.datasync.domain.driver.Driver

/**
 * Port for fetching F1 drivers from an external data source.
 */
interface F1DriverFetcher {
    fun forEachPageOfDrivers(
        startOffset: Int = 0,
        onPage: (PageFetchResult<Driver>) -> Unit,
    ): PaginationSummary
}
