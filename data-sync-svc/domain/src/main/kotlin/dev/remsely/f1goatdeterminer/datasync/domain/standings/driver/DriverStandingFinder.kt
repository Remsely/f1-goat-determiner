package dev.remsely.f1goatdeterminer.datasync.domain.standings.driver

interface DriverStandingFinder {
    fun findByGrandPrixId(grandPrixId: Int): List<DriverStanding>
    fun findByDriverId(driverId: Int): List<DriverStanding>
    fun findFinalStandingsBySeason(season: Int): List<DriverStanding>
    fun count(): Long
}
