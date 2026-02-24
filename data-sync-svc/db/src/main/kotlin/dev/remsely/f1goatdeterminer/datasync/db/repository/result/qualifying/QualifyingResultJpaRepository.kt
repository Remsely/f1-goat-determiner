package dev.remsely.f1goatdeterminer.datasync.db.repository.result.qualifying

import dev.remsely.f1goatdeterminer.datasync.db.entity.result.qualifying.QualifyingResultEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface QualifyingResultJpaRepository : JpaRepository<QualifyingResultEntity, Int> {
    fun findByRaceId(raceId: Int): List<QualifyingResultEntity>
    fun findByDriverId(driverId: Int): List<QualifyingResultEntity>
}
