package dev.remsely.f1goatdeterminer.datasync.db.repository.standings.driver

import dev.remsely.f1goatdeterminer.datasync.domain.standings.driver.DriverStanding
import dev.remsely.f1goatdeterminer.datasync.domain.standings.driver.DriverStandingFinder
import dev.remsely.f1goatdeterminer.datasync.domain.standings.driver.DriverStandingPersister
import org.springframework.stereotype.Service

@Service
class DriverStandingDao(
    private val jpaRepository: DriverStandingJpaRepository,
    private val jdbcRepository: DriverStandingJdbcRepository,
) : DriverStandingFinder, DriverStandingPersister {

    override fun count(): Long = jpaRepository.count()

    override fun upsertAll(standings: List<DriverStanding>): Int = jdbcRepository.upsertAll(standings)
}
