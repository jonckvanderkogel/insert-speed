package com.example.inserts.historicmtm.persistence

import oracle.jdbc.OracleConnection
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Connection
import java.sql.Date
import java.time.LocalDate

@Component
class FooHistoricRepository(private val jdbc: JdbcTemplate) {

    private val bulkBlock = """
        DECLARE
          v_rep_date DATE            := :1;
          v_batch    foo_hist_in_tab := :2;
          
          v_old_hash VARCHAR2(64);
          v_new_hash VARCHAR2(64);
          v_bk_hash  VARCHAR2(64);
        BEGIN
          FOR i IN 1 .. v_batch.COUNT LOOP
            SELECT STANDARD_HASH(v_batch(i).c1||'|'||v_batch(i).c2,'SHA256'),
                   STANDARD_HASH(v_batch(i).bk1||'|'||v_batch(i).bk2,'SHA256')
              INTO v_new_hash, v_bk_hash
              FROM dual;
  
            /* live row (if any) */
            SELECT MIN(content_hash)
              INTO v_old_hash
              FROM foo_historic
             WHERE business_key_hash = v_bk_hash
               AND valid_until IS NULL;
  
            IF v_old_hash IS NULL THEN
               INSERT INTO foo_historic (
                 business_key_hash, content_hash,
                 valid_from, valid_until,
                 bk1, bk2, c1, c2 )
               VALUES (
                 v_bk_hash, v_new_hash,
                 v_rep_date, NULL,
                 v_batch(i).bk1, v_batch(i).bk2,
                 v_batch(i).c1,  v_batch(i).c2 );
  
            ELSIF v_old_hash != v_new_hash THEN
               UPDATE foo_historic
                  SET valid_until = v_rep_date
                WHERE business_key_hash = v_bk_hash
                  AND valid_until IS NULL;
  
               INSERT INTO foo_historic (
                 business_key_hash, content_hash,
                 valid_from, valid_until,
                 bk1, bk2, c1, c2 )
               VALUES (
                 v_bk_hash, v_new_hash,
                 v_rep_date, NULL,
                 v_batch(i).bk1, v_batch(i).bk2,
                 v_batch(i).c1,  v_batch(i).c2 );
            END IF;
  
          END LOOP;
        END;
    """.trimIndent()

    fun batchUpsert(batch: List<FooEvent>, repDate: LocalDate) {
        jdbc.execute { con: Connection ->
            val ora = con.unwrap(OracleConnection::class.java)

            ora.prepareCall(bulkBlock).use { cs ->
                cs.setDate (1, Date.valueOf(repDate))
                cs.setArray(2, OracleArrayHelper.fooStructs(ora, batch))
                cs.execute()
            }
            null
        }
    }
}