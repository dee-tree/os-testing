package edu.shamalov.os

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.newFixedThreadPoolContext

class Processor(val cores: UInt = DEFAULT_PROCESSOR_CORES) : AutoCloseable {
    init {
        require(cores > 0u)
    }

    var currentTasksCount = 0
        private set


    @OptIn(DelicateCoroutinesApi::class)
    private val dispatcher = newFixedThreadPoolContext(cores.toInt(), "processor")

    fun CoroutineScope.execute(task: Task) = async(dispatcher) {
        logger.debug { "Execute $task" }
        try {
            currentTasksCount++
            task.onEvent(Event.Start)
        } finally {
            currentTasksCount--
        }
    }

    override fun close() {
        dispatcher.close()
    }

    companion object {
        const val DEFAULT_PROCESSOR_CORES = 1u
    }
}

private val logger = KotlinLogging.logger { }