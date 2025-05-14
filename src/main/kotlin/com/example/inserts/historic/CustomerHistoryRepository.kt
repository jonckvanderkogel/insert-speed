package com.example.inserts.historic

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import kotlin.system.measureNanoTime

@Component
class CustomerHistoryRepository(
    private val jdbcTemplate: JdbcTemplate
) {
    fun batchUpsert(events: List<CustomerEvent>): Long =
        measureNanoTime {
            val sql = """
                DECLARE
                  v_ssn               VARCHAR2(20)  := ?;
                  v_region            VARCHAR2(20)  := ?;
                  v_address           VARCHAR2(255) := ?;
                  v_email             VARCHAR2(255) := ?;
                
                  v_old_content_hash    VARCHAR2(64);
                  v_new_content_hash    VARCHAR2(64);
                  v_business_key_hash   VARCHAR2(64);
                  v_pk_hash             VARCHAR2(64);
                  v_now                 TIMESTAMP     := CURRENT_TIMESTAMP;
                BEGIN
                  -- Compute new content and business key hashes via SELECT FROM DUAL
                  SELECT
                    STANDARD_HASH(v_address || '|' || v_email, 'SHA256'),
                    STANDARD_HASH(v_ssn    || '|' || v_region,  'SHA256')
                  INTO
                    v_new_content_hash,
                    v_business_key_hash
                  FROM DUAL;
                
                  -- Compute primary key hash
                  SELECT
                    STANDARD_HASH(
                      v_business_key_hash ||
                      TO_CHAR(v_now, 'YYYY-MM-DD"T"HH24:MI:SSXFF'),
                      'SHA256'
                    )
                  INTO v_pk_hash
                  FROM DUAL;
                
                  -- Retrieve existing content hash (NULL if none)
                  SELECT MIN(content_hash)
                    INTO v_old_content_hash
                    FROM customer_history
                   WHERE business_key_hash = v_business_key_hash
                     AND valid_until IS NULL;
                
                  IF v_old_content_hash IS NULL THEN
                    -- First insert
                    INSERT INTO customer_history (
                      pk_hash, business_key_hash, content_hash,
                      valid_from, valid_until,
                      ssn, region, address, email
                    ) VALUES (
                      v_pk_hash, v_business_key_hash, v_new_content_hash,
                      v_now, NULL,
                      v_ssn, v_region, v_address, v_email
                    );
                  ELSIF v_old_content_hash != v_new_content_hash THEN
                    -- Close and insert new version
                    UPDATE customer_history
                       SET valid_until = v_now
                     WHERE business_key_hash = v_business_key_hash
                       AND valid_until IS NULL;
                
                    INSERT INTO customer_history (
                      pk_hash, business_key_hash, content_hash,
                      valid_from, valid_until,
                      ssn, region, address, email
                    ) VALUES (
                      v_pk_hash, v_business_key_hash, v_new_content_hash,
                      v_now, NULL,
                      v_ssn, v_region, v_address, v_email
                    );
                  END IF;
                END;
                """
            jdbcTemplate.batchUpdate(
                sql,
                events,
                events.size
            ) { ps, event ->
                ps.setString(1, event.ssn)
                ps.setString(2, event.region)
                ps.setString(3, event.address)
                ps.setString(4, event.email)
            }
        }
}