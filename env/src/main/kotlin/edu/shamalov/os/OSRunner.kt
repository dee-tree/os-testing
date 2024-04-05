package edu.shamalov.os

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlin.random.Random

class OSRunner(
    private val os: OperatingSystem = OperatingSystem(),
    private val random: Random = Random.Default
): AutoCloseable {

    private var isClosed = false

    suspend fun start() = coroutineScope { with(os) { start() } }

    suspend fun emit(action: Action) {
        logger.trace { "Received $action" }
        when (action) {
            is Action.EnqueueTask -> os.enqueueTask(generateTask<Task>(random))
            is Action.EnqueueBasicTask -> os.enqueueTask(generateTask<BasicTask>(random))
            is Action.EnqueueExtendedTask -> os.enqueueTask(generateTask<ExtendedTask>(random))
            is Action.Stop -> close()
        }
    }

    override fun close() {
        if (isClosed) return
        logger.info { "Simulation is finished" }
        Thread.sleep(10000)
        os.stop()
    }

    companion object {
        private val logger = KotlinLogging.logger("OS Simulation")
    }
}
