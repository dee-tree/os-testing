package edu.shamalov.os

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.random.Random

fun main(args: Array<String>) {
    val isManualSimulation = args.getOrNull(0)?.toBooleanStrictOrNull() ?: false
    val random = when (args.size) {
        1 -> args[0].toIntOrNull()?.let { Random(it) } ?: Random.Default
        2 -> args[1].toIntOrNull()?.let { Random(it) } ?: Random.Default
        else -> Random.Default
    }
    val runner = OSRunner(random = random)

    runBlocking {
        launch(Dispatchers.IO) {
            runner.start()
        }

        launch(Dispatchers.IO) {
            when (isManualSimulation) {
                true -> runner.simulateManually()
                false -> runner.simulateAutomatically(random)
            }
            runner.emit(Action.Stop)
        }
    }
}

private suspend fun OSRunner.simulateAutomatically(random: Random, tasks: UInt = 100u) {
    suspend fun suspend() {
        delay((RUNNING_MILLIS_RANGE.random(random) * ACCELERATION_COEFFICIENT).toLong() * 3)
    }

    var fuel = tasks

    while (fuel != 0u) {
        suspend()
        emit(Action.EnqueueTask)
        if (fuel != UInt.MAX_VALUE) fuel--
    }
}

private suspend fun OSRunner.simulateManually() {
    while (true) {
        val input = readlnOrNull()
        when {
            input?.equals("basic", ignoreCase = true) == true -> emit(Action.EnqueueBasicTask)
            input?.equals("extended", ignoreCase = true) == true -> emit(Action.EnqueueExtendedTask)
            input?.equals("task", ignoreCase = true) == true -> emit(Action.EnqueueTask)
            input?.equals("stop", ignoreCase = true) == true -> {
                emit(Action.Stop)
                break
            }
        }
    }
}
