package dev.remsely.f1goatdeterminer.datasync.db.repository.sync.checkpoint

import dev.remsely.f1goatdeterminer.datasync.db.entity.sync.checkpoint.toDomain
import dev.remsely.f1goatdeterminer.datasync.db.entity.sync.checkpoint.toEntity
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointFinder
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointPersister
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class SyncCheckpointDao(
    private val jpaRepository: SyncCheckpointJpaRepository,
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

    override fun updateProgress(id: Long, lastOffset: Int?, lastSeason: Int?, lastRound: Int?, recordsSynced: Int) =
        jpaRepository.updateProgress(id, lastOffset, lastSeason, lastRound, recordsSynced)

    override fun incrementRetryCount(id: Long) = jpaRepository.incrementRetryCount(id)

    override fun complete(id: Long, recordsSynced: Int, completedAt: LocalDateTime) =
        jpaRepository.complete(id, recordsSynced, completedAt)

    override fun fail(id: Long, errorMessage: String) = jpaRepository.fail(id, errorMessage)
}
