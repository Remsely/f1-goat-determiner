package dev.remsely.f1goatdeterminer.datasync.domain.sync

enum class SyncStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    PAUSED
}
