package dev.remsely.f1goatdeterminer.datasync.db.repository.driver

import dev.remsely.f1goatdeterminer.datasync.db.entity.driver.toDomain
import dev.remsely.f1goatdeterminer.datasync.domain.driver.Driver
import dev.remsely.f1goatdeterminer.datasync.domain.driver.DriverFinder
import dev.remsely.f1goatdeterminer.datasync.domain.driver.DriverPersister
import org.springframework.stereotype.Service

@Service
class DriverDao(
    private val jpaRepository: DriverJpaRepository,
    private val jdbcRepository: DriverJdbcRepository,
) : DriverFinder, DriverPersister {

    override fun findById(id: Int): Driver? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun count(): Long = jpaRepository.count()

    override fun upsertAll(drivers: List<Driver>): Int = jdbcRepository.upsertAll(drivers)
}
