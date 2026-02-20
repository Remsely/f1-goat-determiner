package dev.remsely.f1goatdeterminer.datasync.domain.result.race

interface RaceResultPersister {
    fun save(result: RaceResult): RaceResult
    fun saveAll(results: List<RaceResult>): List<RaceResult>
    fun upsertAll(results: List<RaceResult>): Int
}
