package dev.remsely.f1goatdeterminer.datasync.domain.result.race

interface RaceResultPersister {
    fun upsertAll(results: List<RaceResult>): Int
}
