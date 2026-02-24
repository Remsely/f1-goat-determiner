package dev.remsely.f1goatdeterminer.datasync.db.repository.sync.job

import dev.remsely.f1goatdeterminer.datasync.db.entity.sync.job.SyncJobEntity
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJob
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface SyncJobJpaRepository : JpaRepository<SyncJobEntity, Long> {

    fun findFirstByOrderByStartedAtDesc(): SyncJobEntity?

    fun findByStatus(status: SyncStatus): List<SyncJobEntity>

    fun findByStatusIn(statuses: List<SyncStatus>): List<SyncJobEntity>

    @Query(
        """
        SELECT j FROM SyncJobEntity j
        WHERE j.type = :type
        AND j.status IN ('PENDING', 'IN_PROGRESS')
    """,
    )
    fun findActiveByType(type: SyncJob.Type): SyncJobEntity?

    @Query(
        """
        SELECT j FROM SyncJobEntity j
        WHERE j.type = :type
        AND j.status IN ('PAUSED', 'FAILED')
        ORDER BY j.startedAt DESC
    """,
    )
    fun findResumableByType(type: SyncJob.Type): List<SyncJobEntity>

    @Modifying
    @Query(
        """
        UPDATE SyncJobEntity j
        SET j.status = :status, j.updatedAt = :updatedAt, j.errorMessage = :errorMessage
        WHERE j.id = :id
    """,
    )
    fun updateStatus(
        id: Long,
        status: SyncStatus,
        updatedAt: LocalDateTime = LocalDateTime.now(),
        errorMessage: String? = null,
    )

    @Modifying
    @Query(
        """
        UPDATE SyncJobEntity j
        SET j.totalRequests = :totalRequests, j.failedRequests = :failedRequests, j.updatedAt = :updatedAt
        WHERE j.id = :id
    """,
    )
    fun updateProgress(
        id: Long,
        totalRequests: Int,
        failedRequests: Int,
        updatedAt: LocalDateTime = LocalDateTime.now(),
    )

    @Modifying
    @Query(
        """
        UPDATE SyncJobEntity j
        SET j.status = :status, j.completedAt = :completedAt, j.updatedAt = :updatedAt
        WHERE j.id = :id
    """,
    )
    fun complete(
        id: Long,
        status: SyncStatus,
        completedAt: LocalDateTime = LocalDateTime.now(),
        updatedAt: LocalDateTime = LocalDateTime.now(),
    )
}
