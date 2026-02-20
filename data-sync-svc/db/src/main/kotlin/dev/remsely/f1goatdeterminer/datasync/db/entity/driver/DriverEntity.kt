package dev.remsely.f1goatdeterminer.datasync.db.entity.driver

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "drivers")
class DriverEntity(
    @Id
    val id: Int,

    @Column
    val ref: String,

    @Column
    val number: Int?,

    @Column
    val code: String?,

    @Column
    val forename: String,

    @Column
    val surname: String,

    @Column
    val dob: LocalDate?,

    @Column
    val nationality: String?,
)
