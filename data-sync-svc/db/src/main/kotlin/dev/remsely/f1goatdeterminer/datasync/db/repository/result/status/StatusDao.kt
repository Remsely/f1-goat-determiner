package dev.remsely.f1goatdeterminer.datasync.db.repository.result.status

import dev.remsely.f1goatdeterminer.datasync.db.entity.result.status.toDomain
import dev.remsely.f1goatdeterminer.datasync.domain.result.status.Status
import dev.remsely.f1goatdeterminer.datasync.domain.result.status.StatusFinder
import dev.remsely.f1goatdeterminer.datasync.domain.result.status.StatusPersister
import org.springframework.stereotype.Service

@Service
class StatusDao(
    private val jpaRepository: StatusJpaRepository,
    private val jdbcRepository: StatusJdbcRepository,
) : StatusFinder, StatusPersister {

    override fun findById(id: Int): Status? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findAll(): List<Status> = jpaRepository.findAll().map { it.toDomain() }

    override fun count(): Long = jpaRepository.count()

    override fun upsertAll(statuses: List<Status>): Int = jdbcRepository.upsertAll(statuses)
}
