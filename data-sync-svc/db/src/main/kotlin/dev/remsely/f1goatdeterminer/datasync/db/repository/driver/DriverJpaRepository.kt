package dev.remsely.f1goatdeterminer.datasync.db.repository.driver

import dev.remsely.f1goatdeterminer.datasync.db.entity.driver.DriverEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DriverJpaRepository : JpaRepository<DriverEntity, Int> {
    fun findByRef(ref: String): DriverEntity?
    fun findByNationality(nationality: String): List<DriverEntity>
}
