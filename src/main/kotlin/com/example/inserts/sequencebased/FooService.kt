package com.example.inserts.sequencebased

import com.example.inserts.saveAndReturn
import org.springframework.stereotype.Service

@Service
class FooService(
    private val repository: FooRepository
) {
    fun batchInsert(records: List<Foo>): Pair<Long, List<Foo>> = saveAndReturn(
        records,
        repository::saveAll
    )
}