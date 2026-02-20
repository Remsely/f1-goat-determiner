package dev.remsely.f1goatdeterminer.datasync.domain.result.status

interface StatusPersister {
    fun save(status: Status): Status
    fun saveAll(statuses: List<Status>): List<Status>
    fun upsertAll(statuses: List<Status>): Int
}
