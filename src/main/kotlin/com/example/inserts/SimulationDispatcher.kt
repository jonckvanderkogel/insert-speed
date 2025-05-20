package com.example.inserts

import kotlinx.coroutines.runBlocking
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class SimulationDispatcher(
    private val simulations: List<@JvmSuppressWildcards Simulation>,
    private val args: ApplicationArguments
) : ApplicationRunner {
    companion object {
        private val logger = logger()
    }

    override fun run(appArgs: ApplicationArguments) {
        val typeArg = args.getOptionValues("experiment.simulation-type")
            ?.first()
            ?: SimulationType.SEQUENCE.name

        val selected = simulations.find { it.type == SimulationType.fromString(typeArg) }
            ?: error("No simulation found for type=$typeArg")

        logger.info("Dispatching to simulation type='${selected.type}'")
        runBlocking {
            selected.run()      // suspends until the simulation is completely done
        }
    }
}