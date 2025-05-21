package com.example.inserts.historicmtm.simulation

import com.example.inserts.historicmtm.persistence.BarEvent
import com.example.inserts.historicmtm.persistence.FooEvent
import com.example.inserts.historicmtm.persistence.Key
import kotlin.random.Random


object UniverseBuilder {

    data class Universe(
        val fooKeys: List<Key>,
        val barKeys: List<Key>,
        val keyLinks: List<Pair<Key, Key>>,
        val fooInit: List<FooEvent>,
        val barInit: List<BarEvent>
    )

    fun build(
        fooCount: Int,
        barCount: Int
    ): Universe {
        val fooKeys = (1..fooCount).map { "F%05d".format(it) to "${it % 100}" }
        val barKeys = (1..barCount).map { "B%05d".format(it) to "${it % 50}" }

        val fooInit = fooKeys.map { FooEvent(it.first, it.second, "F0", "F1") }
        val barInit = barKeys.map { BarEvent(it.first, it.second, "B0", "B1") }

        val keyLinks = fooKeys.flatMap { fk ->
            barKeys.shuffled()
                .take(Random.nextInt(1, 3))
                .map { bk -> fk to bk }          // purely in business keys
        }

        return Universe(fooKeys, barKeys, keyLinks, fooInit, barInit)
    }

    fun mutateFoo(e: FooEvent, p: Double) =
        if (Random.nextDouble() > p) e
        else e.copy(
            c1 = "F-C1-${Random.nextLong()}",
            c2 = "F-C2-${Random.nextLong()}"
        )

    fun mutateBar(e: BarEvent, p: Double) =
        if (Random.nextDouble() > p) e
        else e.copy(
            c1 = "B-C1-${Random.nextLong()}",
            c2 = "B-C2-${Random.nextLong()}"
        )
}