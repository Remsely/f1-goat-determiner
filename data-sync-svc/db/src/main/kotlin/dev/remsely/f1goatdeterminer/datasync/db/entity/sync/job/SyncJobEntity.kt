package dev.remsely.f1goatdeterminer.datasync.db.entity.sync.job

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJob
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "sync_jobs")
class SyncJobEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val type: SyncJob.Type,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: SyncStatus,

    @Column(nullable = false)
    val startedAt: LocalDateTime,

    @Column(nullable = false)
    var updatedAt: LocalDateTime,

    @Column
    var completedAt: LocalDateTime?,

    @Column
    var errorMessage: String?,

    @Column(nullable = false)
    var totalRequests: Int,

    @Column(nullable = false)
    var failedRequests: Int,
)
