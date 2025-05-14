package com.example.inserts.sequencebased

import org.springframework.stereotype.Service
import kotlin.system.measureNanoTime

@Service
class FooSequenceBasedService(
    private val repository: FooSequenceBasedRepository
) {
    fun batchInsert(records: List<FooSequenceBased>): Long {
        return measureNanoTime {
            repository.saveAll(records)
        }
    }
}