package com.example.inserts

interface Simulation {
    val type: SimulationType
    suspend fun run()
}

enum class SimulationType {
    SEQUENCE, HISTORIC, HISTORIC_MTM;

    companion object {
        fun fromString(type: String): SimulationType = entries
            .find {
                it.name.equals(type, ignoreCase = true)
            }
            ?: SEQUENCE
    }
}