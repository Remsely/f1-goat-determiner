package dev.remsely.f1goatdeterminer.datasync.jolpica.client

import dev.remsely.f1goatdeterminer.datasync.jolpica.api.JolpicaApi
import dev.remsely.f1goatdeterminer.datasync.jolpica.config.JolpicaClientProperties
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.CircuitDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.ConstructorDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.DriverDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.JolpicaResponse
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.MRData
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.RaceDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.StandingsListDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.StatusDto
import io.github.resilience4j.retry.Retry
import org.springframework.stereotype.Component

@Component
class JolpicaApiClient(
    private val api: JolpicaApi,
    private val retry: Retry,
    private val properties: JolpicaClientProperties,
) {
    fun fetchAllStatuses(startOffset: Int = 0): List<StatusDto> =
        paginateAll(startOffset) { offset, limit -> api.fetchStatuses(limit, offset) }
            .flatMap { it.statusTable?.statuses.orEmpty() }

    fun fetchAllCircuits(startOffset: Int = 0): List<CircuitDto> =
        paginateAll(startOffset) { offset, limit -> api.fetchCircuits(limit, offset) }
            .flatMap { it.circuitTable?.circuits.orEmpty() }

    fun fetchAllConstructors(startOffset: Int = 0): List<ConstructorDto> =
        paginateAll(startOffset) { offset, limit -> api.fetchConstructors(limit, offset) }
            .flatMap { it.constructorTable?.constructors.orEmpty() }

    fun fetchAllDrivers(startOffset: Int = 0): List<DriverDto> =
        paginateAll(startOffset) { offset, limit -> api.fetchDrivers(limit, offset) }
            .flatMap { it.driverTable?.drivers.orEmpty() }

    fun fetchAllSeasons(): List<Int> =
        paginateAll(startOffset = 0) { offset, limit -> api.fetchSeasons(limit, offset) }
            .flatMap { it.seasonTable?.seasons.orEmpty() }
            .mapNotNull { it.season.toIntOrNull() }

    fun fetchRaces(season: Int, startOffset: Int = 0): List<RaceDto> =
        paginateAll(startOffset) { offset, limit -> api.fetchRaces(season, limit, offset) }
            .flatMap { it.raceTable?.races.orEmpty() }

    fun fetchResults(season: Int, round: Int): List<RaceDto> =
        retryCall { api.fetchResults(season, round) }
            .mrData.raceTable?.races.orEmpty()

    fun fetchQualifying(season: Int, round: Int): List<RaceDto> =
        retryCall { api.fetchQualifying(season, round) }
            .mrData.raceTable?.races.orEmpty()

    fun fetchDriverStandings(season: Int, round: Int): List<StandingsListDto> =
        retryCall { api.fetchDriverStandings(season, round) }
            .mrData.standingsTable?.standingsLists.orEmpty()

    fun fetchConstructorStandings(season: Int, round: Int): List<StandingsListDto> =
        retryCall { api.fetchConstructorStandings(season, round) }
            .mrData.standingsTable?.standingsLists.orEmpty()

    private fun paginateAll(
        startOffset: Int,
        fetcher: (offset: Int, limit: Int) -> JolpicaResponse,
    ): List<MRData> {
        val pageSize = properties.pageSize
        val result = mutableListOf<MRData>()
        var offset = startOffset

        do {
            val response = retryCall { fetcher(offset, pageSize) }
            val data = response.mrData
            result.add(data)
            offset += pageSize
        } while (offset < data.totalInt)

        return result
    }

    private fun retryCall(call: () -> JolpicaResponse): JolpicaResponse =
        Retry.decorateSupplier(retry, call).get()
}
