package dev.remsely.f1goatdeterminer.datasync.db.entity.result.status

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "statuses")
class StatusEntity(
    @Id
    val id: Int,

    @Column(nullable = false)
    val status: String,
)
