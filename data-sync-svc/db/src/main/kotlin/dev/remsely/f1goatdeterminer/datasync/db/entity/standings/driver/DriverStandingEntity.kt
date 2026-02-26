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

    @Column(nullable = false)
    val raceId: Int,

    @Column(nullable = false)
    val driverId: Int,

    @Column(nullable = false)
    val points: BigDecimal,

    @Column(nullable = false)
    val position: Int,

    @Column(nullable = false)
    val positionText: String,

    @Column(nullable = false)
    val wins: Int,
)
