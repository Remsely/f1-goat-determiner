package dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType

interface SyncCheckpointFinder {
    fun findById(id: Long): SyncCheckpoint?
    fun findByJobId(jobId: Long): List<SyncCheckpoint>
    fun findByJobIdAndEntityType(jobId: Long, entityType: SyncEntityType): SyncCheckpoint?
    fun findPendingByJobId(jobId: Long): List<SyncCheckpoint>
}
