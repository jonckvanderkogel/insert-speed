package com.example.inserts.sequencebased

import org.springframework.data.jpa.repository.JpaRepository

interface BarRepository : JpaRepository<Bar, Long>