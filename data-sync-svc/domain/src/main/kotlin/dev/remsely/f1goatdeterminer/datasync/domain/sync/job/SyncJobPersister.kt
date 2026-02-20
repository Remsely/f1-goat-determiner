package dev.remsely.f1goatdeterminer.datasync.domain.sync.job

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import java.time.LocalDateTime

interface SyncJobPersister {
    fun save(job: SyncJob): SyncJob
    fun updateStatus(id: Long, status: SyncStatus, errorMessage: String? = null)
    fun updateProgress(id: Long, totalRequests: Int, failedRequests: Int)
    fun complete(id: Long, status: SyncStatus, completedAt: LocalDateTime = LocalDateTime.now())
}
