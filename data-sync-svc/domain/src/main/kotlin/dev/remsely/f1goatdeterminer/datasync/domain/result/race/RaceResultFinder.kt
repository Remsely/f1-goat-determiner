package dev.remsely.f1goatdeterminer.datasync.domain.result.race

interface RaceResultFinder {
    fun findByGrandPrixId(grandPrixId: Int): List<RaceResult>
    fun findByDriverId(driverId: Int): List<RaceResult>
    fun findByGrandPrixIdAndDriverId(grandPrixId: Int, driverId: Int): RaceResult?
    fun count(): Long
}
