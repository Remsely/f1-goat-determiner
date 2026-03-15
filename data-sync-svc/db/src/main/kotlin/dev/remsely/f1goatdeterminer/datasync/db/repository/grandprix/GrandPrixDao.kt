package dev.remsely.f1goatdeterminer.datasync.db.repository.grandprix

import dev.remsely.f1goatdeterminer.datasync.db.entity.grandprix.toDomain
import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrix
import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrixFinder
import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrixPersister
import org.springframework.stereotype.Service

@Service
class GrandPrixDao(
    private val jpaRepository: GrandPrixJpaRepository,
    private val jdbcRepository: GrandPrixJdbcRepository,
) : GrandPrixFinder, GrandPrixPersister {

    override fun findById(id: Int): GrandPrix? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findBySeasonAndRound(season: Int, round: Int): GrandPrix? =
        jpaRepository.findBySeasonAndRound(season, round)?.toDomain()

    override fun findIdBySeasonAndRound(season: Int, round: Int): Int? =
        jpaRepository.findBySeasonAndRound(season, round)?.id

    override fun findAllSeasonRoundToId(): Map<Pair<Int, Int>, Int> =
        jpaRepository.findAll().associate { (it.season to it.round) to it.id!! }

    override fun findMaxRoundBySeason(season: Int): Int? = jpaRepository.findMaxRoundBySeason(season)

    override fun findAllSeasons(): List<Int> = jpaRepository.findAllSeasons()

    override fun count(): Long = jpaRepository.count()

    override fun upsertAll(grandPrixList: List<GrandPrix>): Int = jdbcRepository.upsertAll(grandPrixList)
}
