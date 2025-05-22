package com.example.inserts.historic

import com.example.inserts.Simulation
import com.example.inserts.SimulationType
import com.example.inserts.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class HistoricSimulation(
    private val service: CustomerHistoryRepository,
    @Value("\${historic.total-records}")
    private val totalRecords: Int,
    @Value("\${historic.batch-size}")
    private val batchSize: Int,
    @Value("\${historic.change-probability}")
    private val changeProbability: Double
) : Simulation {
    companion object {
        private val logger = logger()
    }

    override val type = SimulationType.HISTORIC

    override suspend fun run() {
        logger.info("[historic] Starting: totalRecords=$totalRecords, batchSize=$batchSize, changeProbability=$changeProbability")

        val businessKeys = (1..100).map { idx ->
            "%09d".format(idx) to listOf("EU", "US", "APAC").random()
        }

        val eventSeq = sequence {
            var lastMap = emptyMap<Pair<String, String>, CustomerEvent>()
            repeat(totalRecords) {
                val key = businessKeys.random()
                val prev = lastMap[key]
                val event = if (prev != null && Random.nextDouble() > changeProbability) {
                    prev
                } else {
                    val newEvent = CustomerEvent(
                        ssn = key.first,
                        region = key.second,
                        address = "Addr-${Random.nextLong().toString(16).take(8)}",
                        email = "${Random.nextBytes(6).joinToString("") { "%02x".format(it) }}@example.com"
                    )
                    lastMap = lastMap + (key to newEvent)
                    newEvent
                }
                yield(event)
            }
        }

        val startTime = System.nanoTime()

        eventSeq
            .take(totalRecords)
            .chunked(batchSize)
            .forEachIndexed { batchIndex, batch ->
                val batchTimeMs = service.batchUpsert(batch) / 1_000_000
                logger.info("[historic] batch=${batchIndex + 1} size=${batch.size} timeMs=$batchTimeMs")
            }

        val totalTimeMs = (System.nanoTime() - startTime) / 1_000_000
        logger.info("[historic] Complete: inserted=$totalRecords totalTimeMs=$totalTimeMs ms")
    }
}