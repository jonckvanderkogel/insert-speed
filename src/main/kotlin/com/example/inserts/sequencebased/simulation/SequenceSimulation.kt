package com.example.inserts.sequencebased.simulation

import com.example.inserts.Simulation
import com.example.inserts.SimulationType
import com.example.inserts.logger
import com.example.inserts.sequencebased.persistence.Bar
import com.example.inserts.sequencebased.persistence.BarService
import com.example.inserts.sequencebased.persistence.Foo
import com.example.inserts.sequencebased.persistence.FooBarService
import com.example.inserts.sequencebased.persistence.FooService
import com.example.inserts.sequencebased.persistence.id
import jakarta.persistence.EntityManager
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
    @Value("\${sequence.total-records}")
    private val totalRecords: Int,
    @Value("\${sequence.batch-size}")
    private val batchSize: Int,
    private val entityManager: EntityManager
) : Simulation {
    companion object {
        private val logger = logger()
    }

    override val type = SimulationType.SEQUENCE

    override suspend fun run() {
        logger.info("[sequence] Starting: totalRecords=$totalRecords, batchSize=$batchSize")

        val startTime = System.nanoTime()

        val totalInserts = (0 until totalRecords / batchSize).asFlow()
            .map { createBatch(batchSize) }
            .withIndex()
            .map { (index, batch) ->
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
                foo1 = "foo1-${Random.Default.nextInt()}",
                foo2 = "foo2-${Random.Default.nextInt()}",
                foo3 = "foo3-${Random.Default.nextInt()}",
                fooBars = emptySet()
            )
        }
        val bars = List(size) { idx ->
            Bar(
                bar1 = "bar1-${Random.Default.nextInt()}",
                bar2 = "bar2-${Random.Default.nextInt()}",
                bar3 = "bar3-${Random.Default.nextInt()}",
                fooBars = emptySet()
            )
        }

        return Batch(foos, bars)
    }

    private suspend fun persistBatchParallel(batch: Batch): PersistInfo = coroutineScope {

        val fooDeferred = async(Dispatchers.IO) {
            fooService.batchInsert(batch.foos)
        }

        val barDeferred = async(Dispatchers.IO) {
            barService.batchInsert(batch.bars)
        }

        val (fooTimeNs, foosSaved) = fooDeferred.await()
        val (barTimeNs, barsSaved) = barDeferred.await()

        val (linksTimeNs, linksWritten) = withContext(Dispatchers.IO) {
            fooBarService.batchInsert(
                buildLinks(foosSaved, barsSaved),
                batchSize
            )
        }

        entityManager.clear()

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
                .take(Random.Default.nextInt(1, 3))
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