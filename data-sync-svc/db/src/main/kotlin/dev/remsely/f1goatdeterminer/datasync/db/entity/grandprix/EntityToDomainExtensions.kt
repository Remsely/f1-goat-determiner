package dev.remsely.f1goatdeterminer.datasync.db.entity.grandprix

import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrix

fun GrandPrixEntity.toDomain() = GrandPrix(
    id = id,
    season = season,
    round = round,
    circuitId = circuitId,
    name = name,
    date = date,
    time = time,
)

fun List<GrandPrixEntity>.toDomain(): List<GrandPrix> = map { it.toDomain() }
