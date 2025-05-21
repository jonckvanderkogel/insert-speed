package com.example.inserts.sequencebased

import com.example.inserts.Simulation
import com.example.inserts.SimulationType.SEQUENCE
import com.example.inserts.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import kotlin.math.max
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

    override suspend fun run() {
        logger.info("[sequence] Starting: totalRecords=$totalRecords, batchSize=$batchSize")

        val startTime = System.nanoTime()

        val totalInserts = (0 until totalRecords / batchSize).asFlow()
            .map { createBatch(batchSize) }                 // regular (non-suspending) map
            .withIndex()                                    // adds the index
            .map { (index, batch) ->                        // ← suspending map
                val result = persistBatchParallel(batch)
                logger.info(
                    "[sequence] batch=${index + 1} completed: " +
                            "persisted ${result.foosPersisted} Foos, " +
                            "${result.barsPersisted} Bars, " +
                            "${result.linksPersisted} links, " +
                            "timeMs=${result.howLongInMs}"
                )
                result.totalPersisted()
            }
            .reduce(Int::plus)

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

    private suspend fun persistBatchParallel(batch: Batch): PersistInfo = coroutineScope {

        val (fooTimeNs, foosSaved) = async(Dispatchers.IO) {
            fooService.batchInsert(batch.foos)
        }.await()

        val (barTimeNs, barsSaved) = async(Dispatchers.IO) {
            barService.batchInsert(batch.bars)
        }.await()

        val (linksTimeNs, linksWritten) = withContext(Dispatchers.IO) {
            fooBarService.batchInsert(
                buildLinks(foosSaved, barsSaved),
                batchSize
            )
        }

        PersistInfo(
            foosPersisted = foosSaved.size,
            barsPersisted = barsSaved.size,
            linksPersisted = linksWritten,
            howLongInMs = (max(fooTimeNs, barTimeNs) + linksTimeNs) / 1_000_000,
        )
    }

    private fun buildLinks(foos: List<Foo>, bars: List<Bar>): List<Pair<Long, Long>> =
        foos.flatMap { foo ->
            bars.shuffled()
                .take(Random.nextInt(1, 3))
                .map { bar -> foo.id to bar.id }
        }

    private data class PersistInfo(
        val foosPersisted: Int,
        val barsPersisted: Int,
        val linksPersisted: Int,
        val howLongInMs: Long
    ) {
        fun totalPersisted(): Int = foosPersisted + barsPersisted + linksPersisted
    }
}