package com.example.inserts

import org.slf4j.Logger
import org.slf4j.LoggerFactory

const val BATCH_SIZE: Int = 10000

inline fun <reified T> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)