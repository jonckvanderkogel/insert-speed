package com.example.inserts.historicmtm.persistence

import oracle.jdbc.OracleConnection
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Connection
import java.sql.Date
import java.time.LocalDate

@Component
class FooHistoricRepository(private val jdbc: JdbcTemplate) {

    fun batchUpsert(batch: List<FooEvent>, repDate: LocalDate) {
        jdbc.execute<Unit> { con: Connection ->
            val ora = con.unwrap(OracleConnection::class.java)

            ora.prepareCall("{ call upsert_foo_hist_bulk(?, ?) }").use { cs ->
                cs.setDate(1, Date.valueOf(repDate))
                cs.setArray(2, OracleArrayHelper.fooStructs(ora, batch))
                cs.execute()
            }

            null
        }
    }
}