package dev.remsely.f1goatdeterminer.datasync.usecase.port

import dev.remsely.f1goatdeterminer.datasync.domain.driver.Driver

/**
 * Port for fetching F1 drivers from an external data source.
 */
interface F1DriverFetcher {
    fun fetchAll(startOffset: Int = 0): List<Driver>
}
