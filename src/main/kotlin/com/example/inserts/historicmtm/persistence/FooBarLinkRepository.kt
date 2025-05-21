package com.example.inserts.historicmtm.persistence

import oracle.jdbc.OracleConnection
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Connection

@Component
class FooBarLinkRepository(private val jdbc: JdbcTemplate) {

    fun batchUpsertLinks(batch: List<Pair<Key, Key>>) {
        jdbc.execute<Unit> { con: Connection ->
            val ora = con.unwrap(OracleConnection::class.java)

            ora.prepareCall("{ call upsert_links_bulk(?) }").use { cs ->
                cs.setArray(1, OracleArrayHelper.linkStructs(ora, batch))
                cs.execute()
            }

            null
        }
    }
}