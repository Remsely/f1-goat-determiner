package dev.remsely.f1goatdeterminer.datasync.db.repository.standings.constructor

import dev.remsely.f1goatdeterminer.datasync.db.entity.standings.constructor.ConstructorStandingEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ConstructorStandingJpaRepository : JpaRepository<ConstructorStandingEntity, Int> {
    fun findByRaceId(raceId: Int): List<ConstructorStandingEntity>
    fun findByConstructorId(constructorId: Int): List<ConstructorStandingEntity>

    @Query(
        """
        SELECT cs FROM ConstructorStandingEntity cs
        JOIN GrandPrixEntity gp ON cs.raceId = gp.id
        WHERE gp.season = :season
        AND gp.round = (SELECT MAX(g.round) FROM GrandPrixEntity g WHERE g.season = :season)
    """,
    )
    fun findFinalStandingsBySeason(season: Int): List<ConstructorStandingEntity>
}
