package com.example.inserts.sequencebased

import com.example.inserts.saveAndReturn
import org.springframework.stereotype.Service

@Service
class BarService(
    private val repository: BarRepository
) {
    fun batchInsert(records: List<Bar>): Pair<Long, List<Bar>> = saveAndReturn(
        records,
        repository::saveAll
    )
}