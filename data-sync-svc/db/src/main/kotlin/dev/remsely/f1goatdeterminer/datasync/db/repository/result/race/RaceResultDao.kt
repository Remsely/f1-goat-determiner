package dev.remsely.f1goatdeterminer.datasync.db.repository.result.race

import dev.remsely.f1goatdeterminer.datasync.domain.result.race.RaceResult
import dev.remsely.f1goatdeterminer.datasync.domain.result.race.RaceResultFinder
import dev.remsely.f1goatdeterminer.datasync.domain.result.race.RaceResultPersister
import org.springframework.stereotype.Service

@Service
class RaceResultDao(
    private val jpaRepository: RaceResultJpaRepository,
    private val jdbcRepository: RaceResultJdbcRepository,
) : RaceResultFinder,
    RaceResultPersister {

    override fun count(): Long = jpaRepository.count()

    override fun upsertAll(results: List<RaceResult>): Int = jdbcRepository.upsertAll(results)
}
