package dev.remsely.f1goatdeterminer.datasync.domain.result.qualifying

interface QualifyingResultFinder {
    fun findByGrandPrixId(grandPrixId: Int): List<QualifyingResult>
    fun findByDriverId(driverId: Int): List<QualifyingResult>
    fun count(): Long
}
