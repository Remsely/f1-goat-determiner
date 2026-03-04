package dev.remsely.f1goatdeterminer.datasync.jolpica.adapter

import dev.remsely.f1goatdeterminer.datasync.jolpica.client.JolpicaApiClient
import dev.remsely.f1goatdeterminer.datasync.jolpica.mapper.toFetchedDriverStanding
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1DriverStandingFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedDriverStanding
import org.springframework.stereotype.Component
import kotlin.collections.map
import kotlin.collections.orEmpty

@Component
class JolpicaDriverStandingFetcher(private val client: JolpicaApiClient) : F1DriverStandingFetcher {
    override fun fetchDriverStandings(season: Int, round: Int): List<FetchedDriverStanding> =
        client.fetchDriverStandings(season, round).flatMap { sl ->
            val s = sl.season.toInt()
            val r = sl.round.toInt()
            sl.driverStandings.orEmpty().map { it.toFetchedDriverStanding(s, r) }
        }
}
