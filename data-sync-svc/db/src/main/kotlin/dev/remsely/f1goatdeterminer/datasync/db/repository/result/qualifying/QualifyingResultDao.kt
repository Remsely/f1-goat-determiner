package dev.remsely.f1goatdeterminer.datasync.db.repository.result.qualifying

import dev.remsely.f1goatdeterminer.datasync.domain.result.qualifying.QualifyingResult
import dev.remsely.f1goatdeterminer.datasync.domain.result.qualifying.QualifyingResultFinder
import dev.remsely.f1goatdeterminer.datasync.domain.result.qualifying.QualifyingResultPersister
import org.springframework.stereotype.Service

@Service
class QualifyingResultDao(
    private val jpaRepository: QualifyingResultJpaRepository,
    private val jdbcRepository: QualifyingResultJdbcRepository,
) : QualifyingResultFinder, QualifyingResultPersister {

    override fun count(): Long = jpaRepository.count()

    override fun upsertAll(results: List<QualifyingResult>): Int = jdbcRepository.upsertAll(results)
}
