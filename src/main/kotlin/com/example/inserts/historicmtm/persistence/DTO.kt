package com.example.inserts.historicmtm.persistence

data class FooEvent(val bk1: String, val bk2: String, val c1: String, val c2: String)
data class BarEvent(val bk1: String, val bk2: String, val c1: String, val c2: String)

typealias Key = Pair<String, String>