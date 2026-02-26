package dev.remsely.f1goatdeterminer.datasync.domain.sync.job

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus

interface SyncJobFinder {
    fun findById(id: Long): SyncJob?
    fun findLatest(): SyncJob?
    fun findByStatus(status: SyncStatus): List<SyncJob>
    fun findByStatusIn(statuses: List<SyncStatus>): List<SyncJob>
    fun findActiveByType(type: SyncJob.Type): SyncJob?
    fun findResumable(type: SyncJob.Type): SyncJob?
}
