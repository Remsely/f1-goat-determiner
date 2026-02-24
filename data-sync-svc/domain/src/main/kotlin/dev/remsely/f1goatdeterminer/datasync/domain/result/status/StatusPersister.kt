package dev.remsely.f1goatdeterminer.datasync.domain.result.status

interface StatusPersister {
    fun upsertAll(statuses: List<Status>): Int
}
