package com.example.inserts.sequencebased

import org.springframework.data.jpa.repository.JpaRepository

interface FooSequenceBasedRepository : JpaRepository<FooSequenceBased, Long>