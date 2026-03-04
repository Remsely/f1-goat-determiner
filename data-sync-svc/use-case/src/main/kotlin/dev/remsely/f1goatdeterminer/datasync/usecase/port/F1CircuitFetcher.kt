package dev.remsely.f1goatdeterminer.datasync.usecase.port

import dev.remsely.f1goatdeterminer.datasync.domain.circuit.Circuit

/**
 * Port for fetching F1 circuits from an external data source.
 */
interface F1CircuitFetcher {
    fun fetchAll(startOffset: Int = 0): List<Circuit>
}
