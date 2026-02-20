package dev.remsely.f1goatdeterminer.datasync.db.entity.grandprix

import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrix

fun GrandPrix.toEntity() = GrandPrixEntity(
    id = id,
    season = season,
    round = round,
    circuitId = circuitId,
    name = name,
    date = date,
    time = time,
)

fun List<GrandPrix>.toEntity(): List<GrandPrixEntity> = map { it.toEntity() }
