package dev.remsely.f1goatdeterminer.datasync.usecase.port

import dev.remsely.f1goatdeterminer.datasync.domain.result.status.Status

/**
 * Port for fetching F1 statuses from an external data source.
 */
interface F1StatusFetcher {
    fun fetchAll(startOffset: Int = 0): List<Status>
}
