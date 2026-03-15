package dev.remsely.f1goatdeterminer.datasync.domain.standings.constructor

interface ConstructorStandingPersister {
    fun upsertAll(standings: List<ConstructorStanding>): Int
}
