package com.example.inserts.sequencebased.persistence

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.sql.Statement.SUCCESS_NO_INFO

@Entity
@Table(name = "foo_x_bar")
data class FooBar(
    @EmbeddedId
    val fooBarId: FooBarKey,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("fooId")
    @JoinColumn(name = "foo_id")
    val foo: Foo,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("barId")
    @JoinColumn(name = "bar_id")
    val bar: Bar
)

@Embeddable
data class FooBarKey(
    @Column(name = "foo_id")
    val fooId: Long,

    @Column(name = "bar_id")
    val barId: Long
)

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

@Service
class FooBarService(
    private val repository: FooBarRepository
) {
    fun batchInsert(records: List<Pair<Long, Long>>, batchSize: Int): Pair<Long, Int> {
        val start = System.nanoTime()
        val results = repository.batchInsert(records, batchSize)
        return Pair(System.nanoTime() - start, results)
    }
}