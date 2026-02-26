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

    @GetExchange("/seasons.json")
    fun fetchSeasons(
        @RequestParam("limit") limit: Int,
        @RequestParam("offset") offset: Int,
    ): JolpicaResponse

    @GetExchange("/{season}.json")
    fun fetchRaces(
        @PathVariable season: Int,
        @RequestParam("limit") limit: Int,
        @RequestParam("offset") offset: Int,
    ): JolpicaResponse

    @GetExchange("/{season}/{round}/results.json")
    fun fetchResults(
        @PathVariable season: Int,
        @PathVariable round: Int,
    ): JolpicaResponse

    @GetExchange("/{season}/{round}/qualifying.json")
    fun fetchQualifying(
        @PathVariable season: Int,
        @PathVariable round: Int,
    ): JolpicaResponse

    @GetExchange("/{season}/{round}/driverStandings.json")
    fun fetchDriverStandings(
        @PathVariable season: Int,
        @PathVariable round: Int,
    ): JolpicaResponse

    @GetExchange("/{season}/{round}/constructorStandings.json")
    fun fetchConstructorStandings(
        @PathVariable season: Int,
        @PathVariable round: Int,
    ): JolpicaResponse
}
