package dev.remsely.f1goatdeterminer.datasync.domain.grandprix

interface GrandPrixFinder {
    fun findById(id: Int): GrandPrix?
    fun findBySeason(season: Int): List<GrandPrix>
    fun findBySeasonAndRound(season: Int, round: Int): GrandPrix?
    fun findMaxRoundBySeason(season: Int): Int?
    fun findAllSeasons(): List<Int>
    fun count(): Long
}
