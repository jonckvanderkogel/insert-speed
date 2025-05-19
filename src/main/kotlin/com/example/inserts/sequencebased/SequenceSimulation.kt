package com.example.inserts.sequencebased

import com.example.inserts.Simulation
import com.example.inserts.SimulationType.SEQUENCE
import com.example.inserts.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class SequenceSimulation(
    private val fooService: FooService,
    private val barService: BarService,
    private val fooBarService: FooBarService,
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

        val totalInserts = (1..(totalRecords / batchSize)).asSequence()
            .map { createBatch(batchSize) }
            .mapIndexed { index, batch ->
                val result = persistBatch(batch)
                logger.info("[sequence] batch=${index + 1} completed: persisted ${result.foosPersisted} Foos, ${result.barsPersisted} Bars, ${result.fooBarsPersisted} links, timeMs=${result.howLongInMs}")

                result.totalPersisted()
            }
            .sum()

        val totalTimeMs = (System.nanoTime() - startTime) / 1_000_000
        logger.info("[sequence] Complete: inserted=$totalInserts totalTimeMs=$totalTimeMs ms")
    }

    data class Batch(
        val foos: List<Foo>,
        val bars: List<Bar>
    )

    private fun createBatch(size: Int): Batch {
        val foos = List(size) { idx ->
            Foo(
                foo1 = "foo1-${Random.nextInt()}",
                foo2 = "foo2-${Random.nextInt()}",
                foo3 = "foo3-${Random.nextInt()}",
                fooBars = emptySet()
            )
        }
        val bars = List(size) { idx ->
            Bar(
                bar1 = "bar1-${Random.nextInt()}",
                bar2 = "bar2-${Random.nextInt()}",
                bar3 = "bar3-${Random.nextInt()}",
                fooBars = emptySet()
            )
        }

        return Batch(foos, bars)
    }

    private fun persistBatch(batch: Batch): PersistInfo {
        val foosSaved = batch.foos
            .let { fooService.batchInsert(it) }

        val barsSaved = batch.bars
            .let { barService.batchInsert(it) }

        val links = foosSaved.second.flatMap { foo ->
            barsSaved.second.shuffled()
                .take(Random.nextInt(1, 3))
                .map { bar ->
                    Pair(foo.fooId!!, bar.barId!!)
                }
        }

        val fooBarsSaved = fooBarService.batchInsert(links, batchSize)

        return PersistInfo(
            foosSaved.second.size,
            barsSaved.second.size,
            fooBarsSaved.second,
            (foosSaved.first + barsSaved.first + fooBarsSaved.first) / 1_000_000
        )
    }

    private data class PersistInfo(
        val foosPersisted: Int,
        val barsPersisted: Int,
        val fooBarsPersisted: Int,
        val howLongInMs: Long
    ) {
        fun totalPersisted(): Int = foosPersisted + barsPersisted + fooBarsPersisted
    }
}