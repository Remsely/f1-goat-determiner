package dev.remsely.f1goatdeterminer.datasync.db.entity.sync.checkpoint

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
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
@Table(name = "sync_checkpoints")
@Suppress("LongParameterList")
class SyncCheckpointEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val jobId: Long,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val entityType: SyncEntityType,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: SyncStatus,

    @Column(nullable = false)
    var lastOffset: Int,

    @Column
    var lastSeason: Int?,

    @Column
    var lastRound: Int?,

    @Column(nullable = false)
    var recordsSynced: Int,

    @Column
    var startedAt: LocalDateTime?,

    @Column
    var completedAt: LocalDateTime?,

    @Column
    var errorMessage: String?,

    @Column(nullable = false)
    var retryCount: Int,
)
