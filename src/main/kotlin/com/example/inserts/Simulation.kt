package com.example.inserts

interface Simulation {
    val type: SimulationType
    fun run()
}

enum class SimulationType {
    SEQUENCE, HISTORIC;

    companion object {
        fun fromString(type: String): SimulationType = entries
            .find {
                it.name.equals(type, ignoreCase = true)
            }
            ?: SEQUENCE
    }
}