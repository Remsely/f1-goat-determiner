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

    @Column
    val season: Int,

    @Column
    val round: Int,

    @Column
    val circuitId: Int,

    @Column
    val name: String,

    @Column
    val date: LocalDate,

    @Column
    val time: LocalTime?,
)
