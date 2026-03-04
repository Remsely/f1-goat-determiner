package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.domain.circuit.Circuit
import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.mapper.toDomain
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1CircuitFetcher
import org.springframework.stereotype.Component

@Component
class JolpicaCircuitFetcher(private val client: JolpicaApiClient) : F1CircuitFetcher {
    override fun fetchAll(startOffset: Int): List<Circuit> =
        client.fetchAllCircuits(startOffset).map { it.toDomain() }
}
