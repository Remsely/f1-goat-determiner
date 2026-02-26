package dev.remsely.f1goatdeterminer.datasync.db.repository.sync.checkpoint

import dev.remsely.f1goatdeterminer.datasync.db.entity.sync.checkpoint.SyncCheckpointEntity
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface SyncCheckpointJpaRepository : JpaRepository<SyncCheckpointEntity, Long> {

    fun findByJobId(jobId: Long): List<SyncCheckpointEntity>

    fun findByJobIdAndEntityType(jobId: Long, entityType: SyncEntityType): SyncCheckpointEntity?

    fun findByJobIdAndStatus(jobId: Long, status: SyncStatus): List<SyncCheckpointEntity>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE SyncCheckpointEntity c
        SET c.status = :status, c.errorMessage = :errorMessage
        WHERE c.id = :id
    """,
    )
    fun updateStatus(id: Long, status: SyncStatus, errorMessage: String? = null)

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE SyncCheckpointEntity c
        SET c.lastOffset = COALESCE(:lastOffset, c.lastOffset),
            c.lastSeason = COALESCE(:lastSeason, c.lastSeason),
            c.lastRound = COALESCE(:lastRound, c.lastRound),
            c.recordsSynced = :recordsSynced
        WHERE c.id = :id
    """,
    )
    fun updateProgress(id: Long, lastOffset: Int?, lastSeason: Int?, lastRound: Int?, recordsSynced: Int)

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SyncCheckpointEntity c SET c.retryCount = c.retryCount + 1 WHERE c.id = :id")
    fun incrementRetryCount(id: Long)

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE SyncCheckpointEntity c
        SET c.status = 'COMPLETED', c.recordsSynced = :recordsSynced, c.completedAt = :completedAt
        WHERE c.id = :id
    """,
    )
    fun complete(id: Long, recordsSynced: Int, completedAt: LocalDateTime = LocalDateTime.now())

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE SyncCheckpointEntity c
        SET c.status = 'FAILED', c.errorMessage = :errorMessage
        WHERE c.id = :id
    """,
    )
    fun fail(id: Long, errorMessage: String)
}
