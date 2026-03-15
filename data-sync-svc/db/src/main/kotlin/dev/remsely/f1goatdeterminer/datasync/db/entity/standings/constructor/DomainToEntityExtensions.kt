package dev.remsely.f1goatdeterminer.datasync.db.entity.standings.constructor

import dev.remsely.f1goatdeterminer.datasync.domain.standings.constructor.ConstructorStanding

fun ConstructorStanding.toEntity() = ConstructorStandingEntity(
    id = id,
    raceId = grandPrixId,
    constructorId = constructorId,
    points = points,
    position = position,
    positionText = positionText,
    wins = wins,
)

fun List<ConstructorStanding>.toEntity(): List<ConstructorStandingEntity> = map { it.toEntity() }
