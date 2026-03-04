package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.domain.result.status.Status
import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.mapper.toDomain
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1StatusFetcher
import org.springframework.stereotype.Component

@Component
class JolpicaStatusFetcher(private val client: JolpicaApiClient) : F1StatusFetcher {
    override fun fetchAll(startOffset: Int): List<Status> =
        client.fetchAllStatuses(startOffset).map { it.toDomain() }
}
