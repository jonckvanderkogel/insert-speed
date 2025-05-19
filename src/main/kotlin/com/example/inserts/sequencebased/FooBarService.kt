package com.example.inserts.sequencebased

import org.springframework.stereotype.Service

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