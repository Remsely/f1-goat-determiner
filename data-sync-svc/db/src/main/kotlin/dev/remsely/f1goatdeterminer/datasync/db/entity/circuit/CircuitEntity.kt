package dev.remsely.f1goatdeterminer.datasync.db.entity.circuit

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "circuits")
class CircuitEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Column(nullable = false)
    val ref: String,

    @Column(nullable = false)
    val name: String,

    @Column
    val locality: String?,

    @Column
    val country: String?,
)
