package dev.remsely.f1goatdeterminer.datasync.db.entity.circuit

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "circuits")
class CircuitEntity(
    @Id
    val id: Int,

    @Column
    val ref: String,

    @Column
    val name: String,

    @Column
    val locality: String?,

    @Column
    val country: String?
)
