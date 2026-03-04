package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.mapper.toFetchedRaceResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1RaceResultFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedRaceResult
import org.springframework.stereotype.Component
import kotlin.collections.map
import kotlin.collections.orEmpty

@Component
class JolpicaRaceResultFetcher(private val client: JolpicaApiClient) : F1RaceResultFetcher {
    override fun fetchResults(season: Int, round: Int): List<FetchedRaceResult> =
        client.fetchResults(season, round).flatMap { race ->
            val s = race.season.toInt()
            val r = race.round.toInt()
            race.results.orEmpty().map { it.toFetchedRaceResult(s, r) }
        }
}
