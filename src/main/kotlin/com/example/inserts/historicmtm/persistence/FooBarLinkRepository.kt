package com.example.inserts.historicmtm.persistence

import oracle.jdbc.OracleConnection
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Connection

@Component
class FooBarLinkRepository(private val jdbc: JdbcTemplate) {

    private val linkMergeSql = """
            MERGE
            INTO
                foo_historic_x_bar_historic t
            USING
                (   SELECT
                        fh.business_key_hash AS foo_hash,
                        bh.business_key_hash AS bar_hash,
                        fh.valid_from        AS foo_from,
                        bh.valid_from        AS bar_from
                    FROM
                        TABLE(CAST(? AS link_key_tab)) l
                    JOIN
                        foo_historic fh
                    ON
                        fh.bk1 = l.foo_bk1
                    AND fh.bk2 = l.foo_bk2
                    AND fh.valid_until IS NULL
                    JOIN
                        bar_historic bh
                    ON
                        bh.bk1 = l.bar_bk1
                    AND bh.bk2 = l.bar_bk2
                    AND bh.valid_until IS NULL ) src
            ON
                (
                    t.foo_business_key_hash = src.foo_hash
                AND t.bar_business_key_hash = src.bar_hash
                AND t.foo_valid_from = src.foo_from
                AND t.bar_valid_from = src.bar_from )
            WHEN NOT MATCHED
                THEN
            INSERT
                (
                    foo_business_key_hash,
                    bar_business_key_hash,
                    foo_valid_from,
                    bar_valid_from
                )
                VALUES
                (
                    src.foo_hash,
                    src.bar_hash,
                    src.foo_from,
                    src.bar_from
                )
""".trimIndent()
    fun batchUpsertLinks(batch: List<Pair<Key, Key>>) {
        jdbc.execute { con: Connection ->
            val ora = con.unwrap(OracleConnection::class.java)
            ora.prepareStatement(linkMergeSql).use { ps ->
                ps.setArray(1, OracleArrayHelper.linkStructs(ora, batch))
                ps.executeUpdate()
            }
        }
    }
}