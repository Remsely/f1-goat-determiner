package dev.remsely.f1goatdeterminer.datasync.db.repository.standings.driver

import dev.remsely.f1goatdeterminer.datasync.db.entity.standings.driver.DriverStandingEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface DriverStandingJpaRepository : JpaRepository<DriverStandingEntity, Int> {
    fun findByRaceId(raceId: Int): List<DriverStandingEntity>
    fun findByDriverId(driverId: Int): List<DriverStandingEntity>

    @Query(
        """
        SELECT ds FROM DriverStandingEntity ds
        JOIN GrandPrixEntity gp ON ds.raceId = gp.id
        WHERE gp.season = :season
        AND gp.round = (SELECT MAX(g.round) FROM GrandPrixEntity g WHERE g.season = :season)
    """,
    )
    fun findFinalStandingsBySeason(season: Int): List<DriverStandingEntity>
}
