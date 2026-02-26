package dev.remsely.f1goatdeterminer.datasync.db.repository.sync.job

import dev.remsely.f1goatdeterminer.datasync.db.entity.sync.job.toDomain
import dev.remsely.f1goatdeterminer.datasync.db.entity.sync.job.toEntity
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJob
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobFinder
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobPersister
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class SyncJobDao(
    private val jpaRepository: SyncJobJpaRepository,
) : SyncJobFinder, SyncJobPersister {

    override fun findById(id: Long): SyncJob? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findLatest(): SyncJob? = jpaRepository.findFirstByOrderByStartedAtDesc()?.toDomain()

    override fun findByStatus(status: SyncStatus): List<SyncJob> = jpaRepository.findByStatus(status).toDomain()

    override fun findByStatusIn(statuses: List<SyncStatus>): List<SyncJob> =
        jpaRepository.findByStatusIn(statuses).toDomain()

    override fun findActiveByType(type: SyncJob.Type): SyncJob? = jpaRepository.findActiveByType(type)?.toDomain()

    override fun findResumable(type: SyncJob.Type): SyncJob? =
        jpaRepository.findResumableByType(type).firstOrNull()?.toDomain()

    override fun save(job: SyncJob): SyncJob = jpaRepository.save(job.toEntity()).toDomain()

    override fun updateStatus(id: Long, status: SyncStatus, errorMessage: String?) =
        jpaRepository.updateStatus(id, status, errorMessage = errorMessage)

    override fun updateProgress(id: Long, totalRequests: Int, failedRequests: Int) =
        jpaRepository.updateProgress(id, totalRequests, failedRequests)

    override fun complete(id: Long, status: SyncStatus, completedAt: LocalDateTime) =
        jpaRepository.complete(id, status, completedAt)
}
