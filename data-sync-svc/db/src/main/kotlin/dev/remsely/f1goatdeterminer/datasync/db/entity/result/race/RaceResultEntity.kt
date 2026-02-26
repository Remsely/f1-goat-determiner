package dev.remsely.f1goatdeterminer.datasync.db.entity.result.race

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "results")
@Suppress("LongParameterList")
class RaceResultEntity(
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
    val grid: Int,

    @Column
    val position: Int?,

    @Column(nullable = false)
    val positionText: String,

    @Column(nullable = false)
    val positionOrder: Int,

    @Column(nullable = false)
    val points: BigDecimal,

    @Column(nullable = false)
    val laps: Int,

    @Column
    val time: String?,

    @Column
    val milliseconds: Long?,

    @Column
    val fastestLap: Int?,

    @Column
    val fastestLapRank: Int?,

    @Column
    val fastestLapTime: String?,

    @Column
    val fastestLapSpeed: BigDecimal?,

    @Column(nullable = false)
    val statusId: Int,
)
