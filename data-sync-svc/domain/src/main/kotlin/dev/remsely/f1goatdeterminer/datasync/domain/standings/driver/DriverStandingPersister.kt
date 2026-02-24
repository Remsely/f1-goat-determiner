package dev.remsely.f1goatdeterminer.datasync.domain.standings.driver

interface DriverStandingPersister {
    fun upsertAll(standings: List<DriverStanding>): Int
}
