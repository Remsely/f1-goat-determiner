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
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.RateLimitExhaustedException
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.retry.Retry
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException

private val log = KotlinLogging.logger {}

@Component
class JolpicaApiClient(
    private val api: JolpicaApi,
    private val retry: Retry,
    private val properties: JolpicaClientProperties,
) {
    fun forEachPageOfStatuses(
        startOffset: Int = 0,
        onPage: (items: List<StatusDto>, pageNumber: Int, totalPages: Int, nextOffset: Int) -> Unit,
    ): Int = paginateEachPage(startOffset, { it.statusTable?.statuses.orEmpty() }, { offset, limit ->
        api.fetchStatuses(limit, offset)
    }, onPage)

    fun forEachPageOfCircuits(
        startOffset: Int = 0,
        onPage: (items: List<CircuitDto>, pageNumber: Int, totalPages: Int, nextOffset: Int) -> Unit,
    ): Int = paginateEachPage(startOffset, { it.circuitTable?.circuits.orEmpty() }, { offset, limit ->
        api.fetchCircuits(limit, offset)
    }, onPage)

    fun forEachPageOfConstructors(
        startOffset: Int = 0,
        onPage: (items: List<ConstructorDto>, pageNumber: Int, totalPages: Int, nextOffset: Int) -> Unit,
    ): Int = paginateEachPage(startOffset, { it.constructorTable?.constructors.orEmpty() }, { offset, limit ->
        api.fetchConstructors(limit, offset)
    }, onPage)

    fun forEachPageOfDrivers(
        startOffset: Int = 0,
        onPage: (items: List<DriverDto>, pageNumber: Int, totalPages: Int, nextOffset: Int) -> Unit,
    ): Int = paginateEachPage(startOffset, { it.driverTable?.drivers.orEmpty() }, { offset, limit ->
        api.fetchDrivers(limit, offset)
    }, onPage)

    fun forEachPageOfRaces(
        startOffset: Int = 0,
        onPage: (items: List<RaceDto>, pageNumber: Int, totalPages: Int, nextOffset: Int) -> Unit,
    ): Int = paginateEachPage(startOffset, { it.raceTable?.races.orEmpty() }, { offset, limit ->
        api.fetchAllRaces(limit, offset)
    }, onPage)

    fun forEachPageOfResults(
        startOffset: Int = 0,
        onPage: (items: List<RaceDto>, pageNumber: Int, totalPages: Int, nextOffset: Int) -> Unit,
    ): Int = paginateEachPage(startOffset, { it.raceTable?.races.orEmpty() }, { offset, limit ->
        api.fetchAllResults(limit, offset)
    }, onPage)

    fun forEachPageOfQualifying(
        startOffset: Int = 0,
        onPage: (items: List<RaceDto>, pageNumber: Int, totalPages: Int, nextOffset: Int) -> Unit,
    ): Int = paginateEachPage(startOffset, { it.raceTable?.races.orEmpty() }, { offset, limit ->
        api.fetchAllQualifying(limit, offset)
    }, onPage)

    fun forEachPageOfSeasonDriverStandings(
        season: Int,
        startOffset: Int = 0,
        onPage: (items: List<StandingsListDto>, pageNumber: Int, totalPages: Int, nextOffset: Int) -> Unit,
    ): Int = paginateEachPage(
        startOffset,
        { it.standingsTable?.standingsLists.orEmpty() },
        { offset, limit -> api.fetchSeasonDriverStandings(season, limit, offset) },
        onPage,
    )

    fun forEachPageOfSeasonConstructorStandings(
        season: Int,
        startOffset: Int = 0,
        onPage: (items: List<StandingsListDto>, pageNumber: Int, totalPages: Int, nextOffset: Int) -> Unit,
    ): Int = paginateEachPage(
        startOffset,
        { it.standingsTable?.standingsLists.orEmpty() },
        { offset, limit -> api.fetchSeasonConstructorStandings(season, limit, offset) },
        onPage,
    )

    /**
     * Paginates an API endpoint, calling [onPage] for each fetched page.
     * Returns the total number of API calls made.
     *
     * `totalPages` is recalculated from each response because the API's `total` field
     * can change between requests (e.g. new data added during sync).
     */
    private fun <T> paginateEachPage(
        startOffset: Int,
        itemExtractor: (MRData) -> List<T>,
        fetcher: (offset: Int, limit: Int) -> JolpicaResponse,
        onPage: (items: List<T>, pageNumber: Int, totalPages: Int, nextOffset: Int) -> Unit,
    ): Int {
        val pageSize = properties.pageSize
        var offset = startOffset
        var apiCalls = 0

        do {
            log.debug { ">> API request: offset=$offset, limit=$pageSize (call #${apiCalls + 1})" }
            val response = retryCall { fetcher(offset, pageSize) }
            val data = response.mrData
            val pageItems = itemExtractor(data)
            apiCalls++

            val total = data.totalInt
            val totalPages = if (total > 0) (total + pageSize - 1) / pageSize else 1
            val nextOffset = minOf(offset + pageSize, total)

            log.debug {
                "<< API response: offset=${data.offsetInt}, " +
                    "total=$total, returned=${pageItems.size} (call #$apiCalls, ~$totalPages pages)"
            }
            log.info { "   << $response" }

            if (pageItems.isNotEmpty()) {
                onPage(pageItems, apiCalls, totalPages, nextOffset)
            }

            if (pageItems.isEmpty()) break
            offset = nextOffset
        } while (offset < total)

        return apiCalls
    }

    private fun retryCall(call: () -> JolpicaResponse): JolpicaResponse =
        runCatching { Retry.decorateSupplier(retry, call).get() }
            .getOrElse { e ->
                if (e.isTooManyRequests()) {
                    throw RateLimitExhaustedException(
                        "Jolpica API rate limit exhausted after all retry attempts",
                        e,
                    )
                }
                throw e
            }

    private fun Throwable.isTooManyRequests(): Boolean =
        this is HttpClientErrorException.TooManyRequests ||
            cause is HttpClientErrorException.TooManyRequests
}
