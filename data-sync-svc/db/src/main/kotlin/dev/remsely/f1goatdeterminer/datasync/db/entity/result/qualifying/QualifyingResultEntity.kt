package dev.remsely.f1goatdeterminer.datasync.db.entity.result.qualifying

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "qualifying")
class QualifyingResultEntity(
    @Id
    val id: Int,

    @Column(nullable = false)
    val raceId: Int,

    @Column(nullable = false)
    val driverId: Int,

    @Column(nullable = false)
    val constructorId: Int,

    @Column
    val number: Int?,

    @Column(nullable = false)
    val position: Int,

    @Column
    val q1: String?,

    @Column
    val q2: String?,

    @Column
    val q3: String?,
)
