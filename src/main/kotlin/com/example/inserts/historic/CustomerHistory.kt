package com.example.inserts.historic

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.NamedNativeQuery

@Entity
@Table(name = "customer_history")
@NamedNativeQuery(
    name = "CustomerHistory.upsert",
    query = """
DECLARE
  v_old_content_hash VARCHAR2(64);
  v_new_content_hash VARCHAR2(64) := STANDARD_HASH(:address || '|' || :email, 'SHA256');
  v_now               TIMESTAMP   := CURRENT_TIMESTAMP;
  v_business_key_hash VARCHAR2(64) := STANDARD_HASH(:ssn || '|' || :region, 'SHA256');
  v_pk_hash           VARCHAR2(64) := STANDARD_HASH(
                              v_business_key_hash ||
                              TO_CHAR(v_now, 'YYYY-MM-DD"T"HH24:MI:SSXFF'),
                              'SHA256'
                            );
BEGIN
  -- Retrieve existing content hash if any (only one active row exists)
  SELECT MIN(content_hash)
    INTO v_old_content_hash
    FROM customer_history
   WHERE business_key_hash = v_business_key_hash
     AND valid_until IS NULL;

  IF v_old_content_hash IS NULL THEN
    -- No existing version: first insert
    INSERT INTO customer_history (
      pk_hash,
      business_key_hash,
      content_hash,
      valid_from,
      valid_until,
      ssn,
      region,
      address,
      email
    ) VALUES (
      v_pk_hash,
      v_business_key_hash,
      v_new_content_hash,
      v_now,
      NULL,
      :ssn,
      :region,
      :address,
      :email
    );
  ELSIF v_old_content_hash != v_new_content_hash THEN
    -- Data changed: close previous version and insert new one
    UPDATE customer_history
       SET valid_until = v_now
     WHERE business_key_hash = v_business_key_hash
       AND valid_until IS NULL;

    INSERT INTO customer_history (
      pk_hash,
      business_key_hash,
      content_hash,
      valid_from,
      valid_until,
      ssn,
      region,
      address,
      email
    ) VALUES (
      v_pk_hash,
      v_business_key_hash,
      v_new_content_hash,
      v_now,
      NULL,
      :ssn,
      :region,
      :address,
      :email
    );
  END IF;
END;
""",
    resultClass = CustomerHistory::class
)
data class CustomerHistory(
    @Id
    @Column(name = "pk_hash", length = 64)
    val pkHash: String? = null,

    @Column(name = "ssn", length = 20, nullable = false)
    val ssn: String,

    @Column(name = "region", length = 20, nullable = false)
    val region: String,

    @Column(name = "address", length = 255)
    val address: String,

    @Column(name = "email", length = 255)
    val email: String
)