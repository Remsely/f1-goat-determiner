package dev.remsely.f1goatdeterminer.datasync.db.repository.standings.constructor

import dev.remsely.f1goatdeterminer.datasync.domain.standings.constructor.ConstructorStanding
import dev.remsely.f1goatdeterminer.datasync.domain.standings.constructor.ConstructorStandingFinder
import dev.remsely.f1goatdeterminer.datasync.domain.standings.constructor.ConstructorStandingPersister
import org.springframework.stereotype.Service

@Service
class ConstructorStandingDao(
    private val jpaRepository: ConstructorStandingJpaRepository,
    private val jdbcRepository: ConstructorStandingJdbcRepository,
) : ConstructorStandingFinder,
    ConstructorStandingPersister {

    override fun count(): Long = jpaRepository.count()

    override fun upsertAll(standings: List<ConstructorStanding>): Int = jdbcRepository.upsertAll(standings)
}
