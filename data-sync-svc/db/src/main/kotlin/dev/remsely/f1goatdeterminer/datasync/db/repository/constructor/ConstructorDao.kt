package dev.remsely.f1goatdeterminer.datasync.db.repository.constructor

import dev.remsely.f1goatdeterminer.datasync.db.entity.constructor.toDomain
import dev.remsely.f1goatdeterminer.datasync.domain.constructor.Constructor
import dev.remsely.f1goatdeterminer.datasync.domain.constructor.ConstructorFinder
import dev.remsely.f1goatdeterminer.datasync.domain.constructor.ConstructorPersister
import org.springframework.stereotype.Service

@Service
class ConstructorDao(
    private val jpaRepository: ConstructorJpaRepository,
    private val jdbcRepository: ConstructorJdbcRepository,
) : ConstructorFinder, ConstructorPersister {

    override fun findById(id: Int): Constructor? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun count(): Long = jpaRepository.count()

    override fun upsertAll(constructors: List<Constructor>): Int = jdbcRepository.upsertAll(constructors)
}
