package dev.remsely.f1goatdeterminer.datasync.db.entity.constructor

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "constructors")
class ConstructorEntity(
    @Id
    val id: Int,

    @Column(nullable = false)
    val ref: String,

    @Column(nullable = false)
    val name: String,

    @Column
    val nationality: String?,
)
