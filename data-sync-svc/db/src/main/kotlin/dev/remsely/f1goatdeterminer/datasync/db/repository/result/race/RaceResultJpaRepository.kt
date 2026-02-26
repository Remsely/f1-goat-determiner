package dev.remsely.f1goatdeterminer.datasync.db.repository.result.race

import dev.remsely.f1goatdeterminer.datasync.db.entity.result.race.RaceResultEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RaceResultJpaRepository : JpaRepository<RaceResultEntity, Int> {
    fun findByRaceId(raceId: Int): List<RaceResultEntity>
    fun findByDriverId(driverId: Int): List<RaceResultEntity>
    fun findByRaceIdAndDriverId(raceId: Int, driverId: Int): RaceResultEntity?
}
