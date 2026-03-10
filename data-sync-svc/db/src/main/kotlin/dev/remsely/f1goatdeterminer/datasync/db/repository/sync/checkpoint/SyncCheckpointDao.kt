package dev.remsely.f1goatdeterminer.datasync.db.repository.sync.checkpoint

import dev.remsely.f1goatdeterminer.datasync.db.entity.sync.checkpoint.toDomain
import dev.remsely.f1goatdeterminer.datasync.db.entity.sync.checkpoint.toEntity
import dev.remsely.f1goatdeterminer.datasync.db.repository.sync.job.SyncJobJpaRepository
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointFinder
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointPersister
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SyncCheckpointDao(
    private val jpaRepository: SyncCheckpointJpaRepository,
    private val syncJobJpaRepository: SyncJobJpaRepository,
) : SyncCheckpointFinder, SyncCheckpointPersister {

    override fun findById(id: Long): SyncCheckpoint? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByJobId(jobId: Long): List<SyncCheckpoint> = jpaRepository.findByJobId(jobId).toDomain()

    override fun findByJobIdAndEntityType(jobId: Long, entityType: SyncEntityType): SyncCheckpoint? =
        jpaRepository.findByJobIdAndEntityType(jobId, entityType)?.toDomain()

    override fun findPendingByJobId(jobId: Long): List<SyncCheckpoint> =
        jpaRepository.findByJobIdAndStatus(jobId, SyncStatus.PENDING).toDomain()

    override fun save(checkpoint: SyncCheckpoint): SyncCheckpoint = jpaRepository.save(checkpoint.toEntity()).toDomain()

    override fun saveAll(checkpoints: List<SyncCheckpoint>): List<SyncCheckpoint> =
        jpaRepository.saveAll(checkpoints.map { it.toEntity() }).toDomain()

    override fun updateStatus(id: Long, status: SyncStatus, errorMessage: String?) =
        jpaRepository.updateStatus(id, status, errorMessage)

    @Transactional
    override fun updateProgress(id: Long, lastOffset: Int?, lastSeason: Int?, lastRound: Int?, recordsSynced: Int) {
        jpaRepository.updateProgress(id, lastOffset, lastSeason, lastRound, recordsSynced)
        jpaRepository.findById(id).ifPresent { syncJobJpaRepository.touchUpdatedAt(it.jobId) }
    }

    override fun incrementRetryCount(id: Long) = jpaRepository.incrementRetryCount(id)

    override fun resetRetryCount(id: Long) = jpaRepository.resetRetryCount(id)

    override fun completeWithProgress(
        id: Long,
        lastOffset: Int?,
        lastSeason: Int?,
        lastRound: Int?,
        recordsSynced: Int,
        completedAt: LocalDateTime,
    ) = jpaRepository.completeWithProgress(
        id = id,
        lastOffset = lastOffset,
        lastSeason = lastSeason,
        lastRound = lastRound,
        recordsSynced = recordsSynced,
        completedAt = completedAt,
    )

    override fun fail(id: Long, errorMessage: String) = jpaRepository.fail(id, errorMessage)
}
