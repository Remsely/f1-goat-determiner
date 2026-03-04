package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.domain.constructor.Constructor
import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.mapper.toDomain
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1ConstructorFetcher
import org.springframework.stereotype.Component

@Component
class JolpicaConstructorFetcher(private val client: JolpicaApiClient) : F1ConstructorFetcher {
    override fun fetchAll(startOffset: Int): List<Constructor> =
        client.fetchAllConstructors(startOffset).map { it.toDomain() }
}
