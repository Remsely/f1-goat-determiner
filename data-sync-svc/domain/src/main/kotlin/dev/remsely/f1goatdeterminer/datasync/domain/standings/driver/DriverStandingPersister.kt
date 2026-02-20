package dev.remsely.f1goatdeterminer.datasync.domain.standings.driver

interface DriverStandingPersister {
    fun save(standing: DriverStanding): DriverStanding
    fun saveAll(standings: List<DriverStanding>): List<DriverStanding>
    fun upsertAll(standings: List<DriverStanding>): Int
}
