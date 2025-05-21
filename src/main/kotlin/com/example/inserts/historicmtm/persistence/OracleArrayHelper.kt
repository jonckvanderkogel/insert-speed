package com.example.inserts.historicmtm.persistence

import oracle.jdbc.OracleConnection
import java.sql.Array

object OracleArrayHelper {
    fun fooStructs(ora: OracleConnection, batch: List<FooEvent>): Array =
        ora.createOracleArray(
            "FOO_HIST_IN_TAB",
            batch.map { ev ->
                ora.createStruct(
                    "FOO_HIST_IN_REC",
                    arrayOf(ev.bk1, ev.bk2, ev.c1, ev.c2)
                )
            }.toTypedArray()
        )

    fun barStructs(ora: OracleConnection, batch: List<BarEvent>): Array =
        ora.createOracleArray(
            "BAR_HIST_IN_TAB",
            batch.map { ev ->
                ora.createStruct(
                    "BAR_HIST_IN_REC",
                    arrayOf(ev.bk1, ev.bk2, ev.c1, ev.c2)
                )
            }.toTypedArray()
        )

    fun linkStructs(
        ora: OracleConnection,
        batch: List<Pair<Key, Key>>
    ): Array =
        ora.createOracleArray(
            "LINK_KEY_TAB",
            batch.map { (f, b) ->
                ora.createStruct(
                    "LINK_KEY_REC",
                    arrayOf(f.first, f.second, b.first, b.second)
                )
            }.toTypedArray()
        )
}