package dev.remsely.f1goatdeterminer.datasync.app

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointFinder
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJob
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobFinder
import dev.remsely.f1goatdeterminer.datasync.usecase.command.FullSyncCommand
import dev.remsely.f1goatdeterminer.datasync.usecase.command.IncrementalSyncCommand
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class IncrementalSyncE2eTest : BaseE2eTest() {

    @Autowired
    private lateinit var fullSyncCommand: FullSyncCommand

    @Autowired
    private lateinit var incrementalSyncCommand: IncrementalSyncCommand

    @Autowired
    private lateinit var syncJobFinder: SyncJobFinder

    @Autowired
    private lateinit var checkpointFinder: SyncCheckpointFinder

    @Test
    fun `incremental sync after full sync completes without duplicating existing data`() {
        JolpicaWireMockStubs.setupFullSyncStubs(wireMock)
        fullSyncCommand.execute()

        val initialCounts = currentCounts()
        val fullJob = syncJobFinder.findLatest().shouldNotBeNull()
        fullJob.type shouldBe SyncJob.Type.FULL
        fullJob.status shouldBe SyncStatus.COMPLETED

        wireMock.resetAll()
        stubIncrementalNoChangesResponses(offset = 1, season = 2024)

        incrementalSyncCommand.execute()

        val incrementalJob = syncJobFinder.findLatest().shouldNotBeNull()
        incrementalJob.type shouldBe SyncJob.Type.INCREMENTAL
        incrementalJob.status shouldBe SyncStatus.COMPLETED
        incrementalJob.failedRequests shouldBe 0

        currentCounts() shouldBe initialCounts

        syncJobFinder.findByStatus(SyncStatus.COMPLETED) shouldHaveSize 2

        val checkpoints = checkpointFinder.findByJobId(incrementalJob.id!!)
        checkpoints shouldHaveSize SyncEntityType.syncOrdered.size
        checkpoints.forEach { checkpoint -> checkpoint.status shouldBe SyncStatus.COMPLETED }

        checkpoints.first { it.entityType == SyncEntityType.STATUSES }.lastOffset shouldBe 1
        checkpoints.first { it.entityType == SyncEntityType.RACE_RESULTS }.apply {
            lastOffset shouldBe 1
            lastSeason shouldBe 2024
            recordsSynced shouldBe 0
        }
        checkpoints.first { it.entityType == SyncEntityType.DRIVER_STANDINGS }.apply {
            lastOffset shouldBe 1
            lastSeason shouldBe 2024
            recordsSynced shouldBe 0
        }
    }

    private fun stubIncrementalNoChangesResponses(offset: Int, season: Int) {
        stubEmptyCollection(
            path = "/status.json",
            offset = offset,
            total = 1,
            tableName = "StatusTable",
            collectionName = "Status",
        )
        stubEmptyCollection(
            path = "/circuits.json",
            offset = offset,
            total = 1,
            tableName = "CircuitTable",
            collectionName = "Circuits",
        )
        stubEmptyCollection(
            path = "/constructors.json",
            offset = offset,
            total = 1,
            tableName = "ConstructorTable",
            collectionName = "Constructors",
        )
        stubEmptyCollection(
            path = "/drivers.json",
            offset = offset,
            total = 1,
            tableName = "DriverTable",
            collectionName = "Drivers",
        )
        stubEmptyCollection(
            path = "/races.json",
            offset = offset,
            total = 1,
            tableName = "RaceTable",
            collectionName = "Races",
        )
        stubEmptyCollection(
            path = "/results.json",
            offset = offset,
            total = 1,
            tableName = "RaceTable",
            collectionName = "Races",
        )
        stubEmptyCollection(
            path = "/qualifying.json",
            offset = offset,
            total = 1,
            tableName = "RaceTable",
            collectionName = "Races",
        )
        stubEmptyCollection(
            path = "/$season/driverStandings.json",
            offset = offset,
            total = 1,
            tableName = "StandingsTable",
            collectionName = "StandingsLists",
        )
        stubEmptyCollection(
            path = "/$season/constructorStandings.json",
            offset = offset,
            total = 1,
            tableName = "StandingsTable",
            collectionName = "StandingsLists",
        )
    }

    private fun stubEmptyCollection(
        path: String,
        offset: Int,
        total: Int,
        tableName: String,
        collectionName: String,
    ) {
        wireMock.stubFor(
            get(urlPathEqualTo(path))
                .withQueryParam("offset", equalTo(offset.toString()))
                .withQueryParam("limit", matching("\\d+"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                                "MRData": {
                                    "xmlns": "",
                                    "series": "f1",
                                    "url": "",
                                    "limit": "100",
                                    "offset": "$offset",
                                    "total": "$total",
                                    "$tableName": {
                                        "$collectionName": []
                                    }
                                }
                            }
                            """.trimIndent(),
                        ),
                ),
        )
    }
}
