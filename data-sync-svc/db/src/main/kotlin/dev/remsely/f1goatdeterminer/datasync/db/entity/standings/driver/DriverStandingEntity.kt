package dev.remsely.f1goatdeterminer.datasync.db.entity.standings.driver

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "driver_standings")
class DriverStandingEntity(
    @Id
    val id: Int,

    @Column
    val raceId: Int,

    @Column
    val driverId: Int,

    @Column
    val points: BigDecimal,

    @Column
    val position: Int,

    @Column
    val positionText: String,

    @Column
    val wins: Int,
)
