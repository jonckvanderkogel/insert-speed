package com.example.inserts.historicmtm.persistence

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Date
import java.time.LocalDate

@Component
class BarHistoricRepository(
    private val jdbc: JdbcTemplate,
) {
    private val sql = """
        DECLARE
          v_rep_date DATE        := :p_rep_date;
          v_bk1      VARCHAR2(20):= :p_bk1;
          v_bk2      VARCHAR2(20):= :p_bk2;
          v_c1       VARCHAR2(255):= :p_c1;
          v_c2       VARCHAR2(255):= :p_c2;

          v_old_hash VARCHAR2(64);
          v_new_hash VARCHAR2(64);
          v_bk_hash  VARCHAR2(64);
        BEGIN
          /* compute business-key hash + new content hash */
          v_new_hash := STANDARD_HASH(v_c1||'|'||v_c2,'SHA256');
          v_bk_hash := STANDARD_HASH(v_bk1||'|'||v_bk2,'SHA256');

          /* fetch current live version (if any) */
          SELECT MIN(content_hash)
            INTO v_old_hash
            FROM bar_historic
           WHERE business_key_hash = v_bk_hash
             AND valid_until IS NULL;

          IF v_old_hash IS NULL THEN
            -- first appearance ➜ straight insert
            INSERT INTO bar_historic (
              business_key_hash, content_hash,
              valid_from, valid_until,
              bk1, bk2, c1, c2 )
            VALUES (
              v_bk_hash, v_new_hash,
              v_rep_date, NULL,
              v_bk1, v_bk2, v_c1, v_c2 );

          ELSIF v_old_hash != v_new_hash THEN
            -- close old version & insert new
            UPDATE bar_historic
               SET valid_until = v_rep_date
             WHERE business_key_hash = v_bk_hash
               AND valid_until IS NULL;

            INSERT INTO bar_historic (
              business_key_hash, content_hash,
              valid_from, valid_until,
              bk1, bk2, c1, c2 )
            VALUES (
              v_bk_hash, v_new_hash,
              v_rep_date, NULL,
              v_bk1, v_bk2, v_c1, v_c2 );
          END IF;
        END;
    """.trimIndent()

    fun batchUpsert(events: List<BarEvent>, reportingDate: LocalDate) {
        jdbc.batchUpdate(
            sql,
            events,
            events.size
        ) { ps, ev ->
            ps.setDate  (1, Date.valueOf(reportingDate))
            ps.setString(2, ev.bk1)
            ps.setString(3, ev.bk2)
            ps.setString(4, ev.c1)
            ps.setString(5, ev.c2)
        }
    }
}