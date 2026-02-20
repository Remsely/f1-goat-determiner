package dev.remsely.f1goatdeterminer.datasync.domain.standings.constructor

interface ConstructorStandingPersister {
    fun save(standing: ConstructorStanding): ConstructorStanding
    fun saveAll(standings: List<ConstructorStanding>): List<ConstructorStanding>
    fun upsertAll(standings: List<ConstructorStanding>): Int
}
