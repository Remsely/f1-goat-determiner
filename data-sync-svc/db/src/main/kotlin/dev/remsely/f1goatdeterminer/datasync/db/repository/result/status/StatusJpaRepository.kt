package dev.remsely.f1goatdeterminer.datasync.db.repository.result.status

import dev.remsely.f1goatdeterminer.datasync.db.entity.result.status.StatusEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StatusJpaRepository : JpaRepository<StatusEntity, Int>
