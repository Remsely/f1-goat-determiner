package dev.remsely.f1goatdeterminer.datasync.jolpica.api

import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.JolpicaResponse
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange
interface JolpicaApi {
    @GetExchange("/status.json")
    fun fetchStatuses(
        @RequestParam("limit") limit: Int,
        @RequestParam("offset") offset: Int,
    ): JolpicaResponse

    @GetExchange("/circuits.json")
    fun fetchCircuits(
        @RequestParam("limit") limit: Int,
        @RequestParam("offset") offset: Int,
    ): JolpicaResponse

    @GetExchange("/constructors.json")
    fun fetchConstructors(
        @RequestParam("limit") limit: Int,
        @RequestParam("offset") offset: Int,
    ): JolpicaResponse

    @GetExchange("/drivers.json")
    fun fetchDrivers(
        @RequestParam("limit") limit: Int,
        @RequestParam("offset") offset: Int,
    ): JolpicaResponse

    @GetExchange("/races.json")
    fun fetchAllRaces(
        @RequestParam("limit") limit: Int,
        @RequestParam("offset") offset: Int,
    ): JolpicaResponse

    @GetExchange("/results.json")
    fun fetchAllResults(
        @RequestParam("limit") limit: Int,
        @RequestParam("offset") offset: Int,
    ): JolpicaResponse

    @GetExchange("/qualifying.json")
    fun fetchAllQualifying(
        @RequestParam("limit") limit: Int,
        @RequestParam("offset") offset: Int,
    ): JolpicaResponse

    @GetExchange("/{season}/driverStandings.json")
    fun fetchSeasonDriverStandings(
        @PathVariable season: Int,
        @RequestParam("limit") limit: Int,
        @RequestParam("offset") offset: Int,
    ): JolpicaResponse

    @GetExchange("/{season}/constructorStandings.json")
    fun fetchSeasonConstructorStandings(
        @PathVariable season: Int,
        @RequestParam("limit") limit: Int,
        @RequestParam("offset") offset: Int,
    ): JolpicaResponse
}
