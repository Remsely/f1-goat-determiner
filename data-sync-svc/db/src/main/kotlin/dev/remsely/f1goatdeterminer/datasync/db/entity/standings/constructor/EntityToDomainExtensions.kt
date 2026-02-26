package dev.remsely.f1goatdeterminer.datasync.db.entity.standings.constructor

import dev.remsely.f1goatdeterminer.datasync.domain.standings.constructor.ConstructorStanding

fun ConstructorStandingEntity.toDomain() = ConstructorStanding(
    id = id,
    grandPrixId = raceId,
    constructorId = constructorId,
    points = points,
    position = position,
    positionText = positionText,
    wins = wins,
)

fun List<ConstructorStandingEntity>.toDomain(): List<ConstructorStanding> = map { it.toDomain() }
