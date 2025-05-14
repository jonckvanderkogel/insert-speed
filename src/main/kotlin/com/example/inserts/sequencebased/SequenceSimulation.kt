package com.example.inserts.sequencebased

import com.example.inserts.Simulation
import com.example.inserts.SimulationType.SEQUENCE
import com.example.inserts.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class SequenceSimulation(
    private val service: FooSequenceBasedService,
    @Value("\${experiment.total-records}")
    private val totalRecords: Int,
    @Value("\${experiment.batch-size}")
    private val batchSize: Int
) : Simulation {
    companion object {
        private val logger = logger()
    }

    override val type = SEQUENCE

    override fun run() {
        logger.info("[sequence] Starting: totalRecords=$totalRecords, batchSize=$batchSize")

        val startTime = System.nanoTime()

        generateSequence { randomFoo() }
            .take(totalRecords)
            .chunked(batchSize)
            .forEachIndexed { index, batch ->
                val batchTimeMs = service.batchInsert(batch) / 1_000_000
                logger.info("Inserted batch=${index + 1} size=${batch.size} timeMs=$batchTimeMs")
            }

        val totalTimeMs = (System.nanoTime() - startTime) / 1_000_000
        logger.info("Simulation complete: inserted=$totalRecords totalTimeMs=$totalTimeMs ms")
    }

    private fun randomFoo(): FooSequenceBased =
        FooSequenceBased(
            foo1 = randomString(),
            foo2 = randomString(),
            foo3 = randomString()
        )

    private fun randomString(length: Int = 20): String =
        kotlin.random.Random
            .nextBytes(length)
            .joinToString(separator = "") { "%02x".format(it) }
}