package com.example.inserts.historicmtm.persistence

import oracle.jdbc.OracleConnection
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Connection
import java.sql.Date
import java.time.LocalDate

@Component
class BarHistoricRepository(
    private val jdbc: JdbcTemplate,
) {
    fun batchUpsert(batch: List<BarEvent>, repDate: LocalDate) {
        jdbc.execute<Unit> { con: Connection ->
            val ora = con.unwrap(OracleConnection::class.java)

            ora.prepareCall("{ call upsert_bar_hist_bulk(?, ?) }").use { cs ->
                cs.setDate(1, Date.valueOf(repDate))
                cs.setArray(2, OracleArrayHelper.barStructs(ora, batch))
                cs.execute()
            }

            null
        }
    }
}