package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1SeasonFetcher
import org.springframework.stereotype.Component

@Component
class JolpicaSeasonFetcher(private val client: JolpicaApiClient) : F1SeasonFetcher {
    override fun fetchAllSeasons(): List<Int> = client.fetchAllSeasons()
}
