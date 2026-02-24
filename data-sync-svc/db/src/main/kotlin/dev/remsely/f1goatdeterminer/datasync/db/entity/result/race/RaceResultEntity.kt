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

    @Column
    val raceId: Int,

    @Column
    val driverId: Int,

    @Column
    val constructorId: Int,

    @Column
    val number: Int?,

    @Column
    val grid: Int,

    @Column
    val position: Int?,

    @Column
    val positionText: String,

    @Column
    val positionOrder: Int,

    @Column
    val points: BigDecimal,

    @Column
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

    @Column
    val statusId: Int,
)
