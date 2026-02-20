package dev.remsely.f1goatdeterminer.datasync.domain.standings.constructor

interface ConstructorStandingFinder {
    fun findByGrandPrixId(grandPrixId: Int): List<ConstructorStanding>
    fun findByConstructorId(constructorId: Int): List<ConstructorStanding>
    fun findFinalStandingsBySeason(season: Int): List<ConstructorStanding>
    fun count(): Long
}
