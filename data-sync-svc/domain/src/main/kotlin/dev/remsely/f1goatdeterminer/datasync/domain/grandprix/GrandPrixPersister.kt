package dev.remsely.f1goatdeterminer.datasync.domain.grandprix

interface GrandPrixPersister {
    fun save(grandPrix: GrandPrix): GrandPrix
    fun saveAll(grandPrixList: List<GrandPrix>): List<GrandPrix>
    fun upsertAll(grandPrixList: List<GrandPrix>): Int
}
