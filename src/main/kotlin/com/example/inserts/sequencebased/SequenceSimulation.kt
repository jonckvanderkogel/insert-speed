package com.example.inserts.sequencebased

import com.example.inserts.BATCH_SIZE
import com.example.inserts.Simulation
import com.example.inserts.SimulationType.SEQUENCE
import com.example.inserts.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class SequenceSimulation(
    private val service: FooSequenceBasedService,
    @Value("\${experiment.total-records}")
    private val totalRecords: Int
) : Simulation {
    companion object {
        private val logger = logger()
    }

    override val type = SEQUENCE

    override fun run() {
        logger.info("[sequence] Starting: totalRecords=$totalRecords, batchSize=$BATCH_SIZE")

        val startTime = System.nanoTime()

        generateSequence { randomFoo() }
            .take(totalRecords)
            .chunked(BATCH_SIZE)
            .forEachIndexed { index, batch ->
                val batchTimeMs = service.batchInsert(batch) / 1_000_000
                logger.info("[sequence] batch=${index + 1} size=${batch.size} timeMs=$batchTimeMs")
            }

        val totalTimeMs = (System.nanoTime() - startTime) / 1_000_000
        logger.info("[sequence] Complete: inserted=$totalRecords totalTimeMs=$totalTimeMs ms")
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