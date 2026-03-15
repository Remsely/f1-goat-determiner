package dev.remsely.f1goatdeterminer.datasync.domain.grandprix

interface GrandPrixPersister {
    fun upsertAll(grandPrixList: List<GrandPrix>): Int
}
