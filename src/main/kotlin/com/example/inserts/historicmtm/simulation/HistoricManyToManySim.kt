package com.example.inserts.historicmtm.simulation

import com.example.inserts.Simulation
import com.example.inserts.SimulationType
import com.example.inserts.historicmtm.persistence.FooEvent
import com.example.inserts.historicmtm.persistence.BarEvent
import com.example.inserts.historicmtm.persistence.HistoricProcessor
import com.example.inserts.historicmtm.simulation.UniverseBuilder.mutateBar
import com.example.inserts.historicmtm.simulation.UniverseBuilder.mutateFoo
import com.example.inserts.logger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDate

typealias Key = Pair<String, String>

@Component
class HistoricManyToManySim(
    private val processor: HistoricProcessor,

    @Value("\${mtm.days}")                 private val days: Int,
    @Value("\${mtm.batch-size}")           private val batchSize: Int,
    @Value("\${mtm.change-probability}")   private val pChange: Double
) : Simulation {

    companion object { private val log = logger() }
    override val type = SimulationType.HISTORIC_MTM

    override suspend fun run() = coroutineScope {
        val simStart = System.nanoTime()

        /* ---------- 1. Build static universe -------------------------- */
        val universe = UniverseBuilder.build(days * batchSize, days * batchSize)
        val fooKeys  = universe.fooKeys
        val barKeys  = universe.barKeys
        val links    = universe.keyLinks

        val repStart = LocalDate.parse("2025-05-01")

        upsertEverything(
            universe.fooInit,
            universe.barInit,
            links,
            repStart
        )

        val fooState = universe.fooInit.associateBy { it.bk1 to it.bk2 }.toMutableMap()
        val barState = universe.barInit.associateBy { it.bk1 to it.bk2 }.toMutableMap()

        repeat(days) { idx ->
            val repDate = repStart.plusDays(idx + 1L)

            val fooDaily = fooKeys.map { k ->
                mutateFoo(fooState[k]!!, pChange).also { fooState[k] = it }
            }
            val barDaily = barKeys.map { k ->
                mutateBar(barState[k]!!, pChange).also { barState[k] = it }
            }

            upsertEverything(fooDaily, barDaily, links, repDate)
        }

        log.info("[historic-mtm] completed in ${(System.nanoTime() - simStart)/1_000_000} ms")
    }

    private suspend fun upsertEverything(
        foos: List<FooEvent>,
        bars: List<BarEvent>,
        links: List<Pair<Key, Key>>,
        repDate: LocalDate
    ) = coroutineScope {
        val t0 = System.nanoTime()

        val fooJob = async {
            foos
                .chunked(batchSize)
                .forEach { c -> processor.upsertFoos(c, repDate) }
        }
        val barJob = async {
            bars
                .chunked(batchSize)
                .forEach { c -> processor.upsertBars(c, repDate) }
        }
        fooJob.await(); barJob.await()

        links
            .chunked(batchSize)
            .forEach { c -> processor.upsertLinks(c) }

        log.info(
            "[historic-mtm] $repDate  " +
                    "(${foos.size}+${bars.size}+${links.size} rows) in " +
                    "${(System.nanoTime() - t0)/1_000_000} ms"
        )
    }
}