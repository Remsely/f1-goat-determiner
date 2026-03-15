package dev.remsely.f1goatdeterminer.datasync.db.repository.constructor

import dev.remsely.f1goatdeterminer.datasync.db.entity.constructor.ConstructorEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ConstructorJpaRepository : JpaRepository<ConstructorEntity, Int> {
    fun findByRef(ref: String): ConstructorEntity?
}
