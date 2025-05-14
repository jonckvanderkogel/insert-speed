package com.example.inserts.historic

data class CustomerEvent(
    val ssn: String,
    val region: String,
    val address: String,
    val email: String
)