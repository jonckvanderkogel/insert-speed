package com.example.inserts.sequencebased

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Statement.SUCCESS_NO_INFO

@Component
class FooBarRepository(
    private val template: JdbcTemplate
) {
    fun batchInsert(ids: List<Pair<Long, Long>>, batchSize: Int): Int {
        val updateCounts = template.batchUpdate(
            "INSERT INTO foo_x_bar (foo_id, bar_id) VALUES (?, ?)",
            ids,
            batchSize
        ) { ps, (fooId, barId) ->
            ps.setLong(1, fooId)
            ps.setLong(2, barId)
        }

        return updateCounts.rowsInserted()
    }

    private fun Array<IntArray>.rowsInserted(): Int =
        sumOf { inner ->
            inner.count { it > 0 || it == SUCCESS_NO_INFO }
        }
}
