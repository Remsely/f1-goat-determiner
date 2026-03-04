package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.mapper.toFetchedConstructorStanding
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1ConstructorStandingFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedConstructorStanding
import org.springframework.stereotype.Component
import kotlin.collections.map
import kotlin.collections.orEmpty

@Component
class JolpicaConstructorStandingFetcher(private val client: JolpicaApiClient) : F1ConstructorStandingFetcher {
    override fun fetchConstructorStandings(season: Int, round: Int): List<FetchedConstructorStanding> =
        client.fetchConstructorStandings(season, round).flatMap { sl ->
            val s = sl.season.toInt()
            val r = sl.round.toInt()
            sl.constructorStandings.orEmpty().map { it.toFetchedConstructorStanding(s, r) }
        }
}
