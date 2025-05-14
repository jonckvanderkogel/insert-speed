package com.example.inserts

interface Simulation {
    val type: SimulationType
    fun run()
}

enum class SimulationType {
    SEQUENCE, HISTORIC
}