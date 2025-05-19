package com.example.inserts.sequencebased

import org.springframework.data.jpa.repository.JpaRepository

interface FooRepository : JpaRepository<Foo, Long>