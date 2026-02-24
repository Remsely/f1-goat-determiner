package dev.remsely.f1goatdeterminer.datasync.db.repository.grandprix

import dev.remsely.f1goatdeterminer.datasync.db.entity.grandprix.GrandPrixEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface GrandPrixJpaRepository : JpaRepository<GrandPrixEntity, Int> {
    fun findBySeason(season: Int): List<GrandPrixEntity>
    fun findBySeasonAndRound(season: Int, round: Int): GrandPrixEntity?

    @Query("SELECT MAX(g.round) FROM GrandPrixEntity g WHERE g.season = :season")
    fun findMaxRoundBySeason(season: Int): Int?

    @Query("SELECT DISTINCT g.season FROM GrandPrixEntity g ORDER BY g.season")
    fun findAllSeasons(): List<Int>
}
