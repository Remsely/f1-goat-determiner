package dev.remsely.f1goatdeterminer.datasync.domain.result.qualifying

interface QualifyingResultPersister {
    fun save(result: QualifyingResult): QualifyingResult
    fun saveAll(results: List<QualifyingResult>): List<QualifyingResult>
    fun upsertAll(results: List<QualifyingResult>): Int
}
