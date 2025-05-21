package com.example.inserts.historicmtm.persistence

import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class HistoricProcessor(
    private val fooRepo: FooHistoricRepository,
    private val barRepo: BarHistoricRepository,
    private val linkRepo: FooBarLinkRepository
) {

    fun upsertFoos(fooBatch: List<FooEvent>, repDate: LocalDate) =
        fooRepo.batchUpsert(fooBatch, repDate)

    fun upsertBars(barBatch: List<BarEvent>, repDate: LocalDate) =
        barRepo.batchUpsert(barBatch, repDate)

    fun upsertLinks(linkBatch: List<Pair<Key, Key>>) =
        linkRepo.batchUpsertLinks(linkBatch)
}