package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.mapper.toFetchedGrandPrix
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1GrandPrixFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedGrandPrix
import org.springframework.stereotype.Component

@Component
class JolpicaGrandPrixFetcher(private val client: JolpicaApiClient) : F1GrandPrixFetcher {
    override fun fetchRaces(season: Int, startOffset: Int): List<FetchedGrandPrix> =
        client.fetchRaces(season, startOffset).map { it.toFetchedGrandPrix() }
}
