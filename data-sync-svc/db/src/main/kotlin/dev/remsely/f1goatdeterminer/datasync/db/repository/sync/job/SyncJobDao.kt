package dev.remsely.f1goatdeterminer.datasync.db.repository.sync.job

import dev.remsely.f1goatdeterminer.datasync.db.entity.sync.job.toDomain
import dev.remsely.f1goatdeterminer.datasync.db.entity.sync.job.toEntity
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJob
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobFinder
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobPersister
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SyncJobDao(
    private val jpaRepository: SyncJobJpaRepository,
    private val jdbcTemplate: JdbcTemplate,
) : SyncJobFinder, SyncJobPersister {

    override fun findById(id: Long): SyncJob? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findLatest(): SyncJob? = jpaRepository.findFirstByOrderByIdDesc()?.toDomain()

    override fun findByStatus(status: SyncStatus): List<SyncJob> = jpaRepository.findByStatus(status).toDomain()

    override fun findByStatusIn(statuses: List<SyncStatus>): List<SyncJob> =
        jpaRepository.findByStatusIn(statuses).toDomain()

    override fun findActiveByType(type: SyncJob.Type): SyncJob? = jpaRepository.findActiveByType(type)?.toDomain()

    override fun findResumable(type: SyncJob.Type): SyncJob? =
        jpaRepository.findResumableByType(type).firstOrNull()?.toDomain()

    override fun save(job: SyncJob): SyncJob = jpaRepository.save(job.toEntity()).toDomain()

    @Transactional
    override fun tryCreateJob(type: SyncJob.Type): SyncJob? {
        jdbcTemplate.execute("SELECT pg_advisory_xact_lock(hashtext('sync_job_creation'))")
        if (hasActiveJobs()) return null
        val now = LocalDateTime.now()
        return save(
            SyncJob(
                id = null,
                type = type,
                status = SyncStatus.PENDING,
                startedAt = now,
                updatedAt = now,
                completedAt = null,
                errorMessage = null,
                totalRequests = 0,
                failedRequests = 0,
            ),
        )
    }

    override fun updateStatus(id: Long, status: SyncStatus, errorMessage: String?) =
        jpaRepository.updateStatus(id, status, errorMessage = errorMessage)

    override fun updateProgress(id: Long, totalRequests: Int, failedRequests: Int) =
        jpaRepository.updateProgress(id, totalRequests, failedRequests)

    override fun touchUpdatedAt(id: Long) = jpaRepository.touchUpdatedAt(id)

    override fun complete(id: Long, status: SyncStatus, completedAt: LocalDateTime) =
        jpaRepository.complete(id, status, completedAt)

    @Transactional
    override fun tryClaimResumableJob(updatedBefore: LocalDateTime): SyncJob? =
        claimJob(
            """
            UPDATE sync_jobs SET status = 'PENDING', updated_at = NOW()
            WHERE id = (
                SELECT id FROM sync_jobs
                WHERE status IN ('FAILED', 'PAUSED')
                  AND updated_at < ?
                ORDER BY id DESC
                LIMIT 1
                FOR UPDATE SKIP LOCKED
            )
            RETURNING id, type, status, started_at, updated_at, completed_at,
                      error_message, total_requests, failed_requests
            """,
            updatedBefore,
        )

    @Transactional
    override fun tryClaimStaleJob(updatedBefore: LocalDateTime): SyncJob? =
        claimJob(
            """
            UPDATE sync_jobs SET status = 'PENDING', updated_at = NOW()
            WHERE id = (
                SELECT id FROM sync_jobs
                WHERE status IN ('IN_PROGRESS', 'PENDING')
                  AND updated_at < ?
                ORDER BY id DESC
                LIMIT 1
                FOR UPDATE SKIP LOCKED
            )
            RETURNING id, type, status, started_at, updated_at, completed_at,
                      error_message, total_requests, failed_requests
            """,
            updatedBefore,
        )

    private fun claimJob(sql: String, vararg args: Any): SyncJob? =
        jdbcTemplate.query(sql, { rs, _ ->
            SyncJob(
                id = rs.getLong("id"),
                type = SyncJob.Type.valueOf(rs.getString("type")),
                status = SyncStatus.valueOf(rs.getString("status")),
                startedAt = rs.getTimestamp("started_at").toLocalDateTime(),
                updatedAt = rs.getTimestamp("updated_at").toLocalDateTime(),
                completedAt = rs.getTimestamp("completed_at")?.toLocalDateTime(),
                errorMessage = rs.getString("error_message"),
                totalRequests = rs.getInt("total_requests"),
                failedRequests = rs.getInt("failed_requests"),
            )
        }, *args).firstOrNull()

    override fun hasActiveJobs(): Boolean =
        jpaRepository.findByStatusIn(listOf(SyncStatus.PENDING, SyncStatus.IN_PROGRESS)).isNotEmpty()

    override fun hasCompletedFullSync(): Boolean =
        jpaRepository.existsByTypeAndStatus(SyncJob.Type.FULL, SyncStatus.COMPLETED)

    override fun findLatestCompleted(): SyncJob? =
        jpaRepository.findFirstByStatusOrderByIdDesc(SyncStatus.COMPLETED)?.toDomain()

    @Transactional
    override fun failActiveJobs(reason: String): Int =
        jdbcTemplate.update(
            """
            UPDATE sync_jobs
            SET status = 'FAILED', updated_at = NOW(), error_message = ?
            WHERE status IN ('IN_PROGRESS', 'PENDING')
            """,
            reason,
        )
}
