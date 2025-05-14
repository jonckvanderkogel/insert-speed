package com.example.inserts

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

        val simType = try {
            SimulationType.valueOf(typeArg.uppercase())
        } catch (_: IllegalArgumentException) {
            logger.warn("Unknown simulation type='$typeArg'. Defaulting to SEQUENCE")
            SimulationType.SEQUENCE
        }

        val selected = simulations.find { it.type == simType }
            ?: error("No simulation found for type=$simType")

        logger.info("Dispatching to simulation type='${selected.type}'")
        selected.run()
    }
}