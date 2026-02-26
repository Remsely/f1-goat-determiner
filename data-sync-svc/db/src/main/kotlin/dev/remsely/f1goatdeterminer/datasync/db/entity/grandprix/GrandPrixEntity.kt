package dev.remsely.f1goatdeterminer.datasync.db.entity.grandprix

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalTime

@Entity
@Table(name = "races")
class GrandPrixEntity(
    @Id
    val id: Int,

    @Column(nullable = false)
    val season: Int,

    @Column(nullable = false)
    val round: Int,

    @Column(nullable = false)
    val circuitId: Int,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val date: LocalDate,

    @Column
    val time: LocalTime?,
)
