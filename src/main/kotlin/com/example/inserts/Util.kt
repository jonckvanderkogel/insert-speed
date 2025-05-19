package com.example.inserts

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)

fun <T> saveAndReturn(records: List<T>, persistFun: (List<T>) -> List<T>): Pair<Long, List<T>>{
    val start = System.nanoTime()
    val results = persistFun(records)
    return Pair(System.nanoTime() - start, results)
}