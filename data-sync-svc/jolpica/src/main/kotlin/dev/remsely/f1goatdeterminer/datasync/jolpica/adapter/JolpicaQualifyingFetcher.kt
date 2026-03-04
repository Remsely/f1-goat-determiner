package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.mapper.toFetchedQualifyingResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1QualifyingFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedQualifyingResult
import org.springframework.stereotype.Component
import kotlin.collections.map
import kotlin.collections.orEmpty

@Component
class JolpicaQualifyingFetcher(private val client: JolpicaApiClient) : F1QualifyingFetcher {
    override fun fetchQualifying(season: Int, round: Int): List<FetchedQualifyingResult> =
        client.fetchQualifying(season, round).flatMap { race ->
            val s = race.season.toInt()
            val r = race.round.toInt()
            race.qualifyingResults.orEmpty().map { it.toFetchedQualifyingResult(s, r) }
        }
}
