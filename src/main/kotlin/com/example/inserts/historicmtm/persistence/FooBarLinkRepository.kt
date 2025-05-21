package com.example.inserts.historicmtm.persistence

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class FooBarLinkRepository(private val jdbc: JdbcTemplate) {

    private val sql = """
        DECLARE
          v_foo_bk1   VARCHAR2(20):= :p_foo_bk1;
          v_foo_bk2   VARCHAR2(20):= :p_foo_bk2;
          v_bar_bk1   VARCHAR2(20):= :p_bar_bk1;
          v_bar_bk2   VARCHAR2(20):= :p_bar_bk2;

          v_foo_hash  VARCHAR2(64);
          v_bar_hash  VARCHAR2(64);
          v_foo_from  DATE;
          v_bar_from  DATE;
        BEGIN
          /* --- fetch current live versions -------------------------------- */
          SELECT business_key_hash, valid_from
            INTO v_foo_hash, v_foo_from
            FROM foo_historic
           WHERE bk1 = v_foo_bk1
             AND bk2 = v_foo_bk2
             AND valid_until IS NULL;

          SELECT business_key_hash, valid_from
            INTO v_bar_hash, v_bar_from
            FROM bar_historic
           WHERE bk1 = v_bar_bk1
             AND bk2 = v_bar_bk2
             AND valid_until IS NULL;

          /* --- insert only when not already present ----------------------- */
          MERGE INTO foo_historic_x_bar_historic t
          USING ( SELECT v_foo_hash AS foo_hash,
                         v_bar_hash AS bar_hash,
                         v_foo_from AS foo_from,
                         v_bar_from AS bar_from
                    FROM dual ) src
          ON ( t.foo_business_key_hash = src.foo_hash
               AND t.bar_business_key_hash = src.bar_hash
               AND t.foo_valid_from       = src.foo_from
               AND t.bar_valid_from       = src.bar_from )
          WHEN NOT MATCHED THEN
            INSERT (foo_business_key_hash, bar_business_key_hash,
                    foo_valid_from,       bar_valid_from)
            VALUES (src.foo_hash, src.bar_hash,
                    src.foo_from, src.bar_from);
        END;
    """.trimIndent()

    fun batchUpsertLinks(
        keyPairs: List<Pair<Key, Key>>
    ) {
        jdbc.batchUpdate(
            sql,
            keyPairs,
            keyPairs.size
        ) { ps, (fooK, barK) ->
            ps.setString(1, fooK.first)
            ps.setString(2, fooK.second)
            ps.setString(3, barK.first)
            ps.setString(4, barK.second)
        }
    }
}