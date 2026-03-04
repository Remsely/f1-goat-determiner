package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.domain.driver.Driver
import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.mapper.toDomain
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1DriverFetcher
import org.springframework.stereotype.Component

@Component
class JolpicaDriverFetcher(private val client: JolpicaApiClient) : F1DriverFetcher {
    override fun fetchAll(startOffset: Int): List<Driver> =
        client.fetchAllDrivers(startOffset).map { it.toDomain() }
}
