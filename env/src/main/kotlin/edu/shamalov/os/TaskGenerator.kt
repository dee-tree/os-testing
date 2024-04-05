package edu.shamalov.os

import kotlin.random.Random
import kotlinx.coroutines.delay

inline fun <reified T> generateTask(random: Random = Random.Default): T where T: Task {
    val priority = Priority(PRIORITY_RANGE.random(random))

    val isBasic = when {
        T::class.java.isAssignableFrom(ExtendedTask::class.java) -> false
        T::class.java.isAssignableFrom(BasicTask::class.java) -> true
        else -> random.nextBoolean()
    }

    val duration = (RUNNING_MILLIS_RANGE.random(random) * ACCELERATION_COEFFICIENT).toLong()
    return if (isBasic) {
        BasicTask(
            priority = priority,
            jobPortion = {
                delay(duration)
            }
        ) as T
    } else {
        val waitingEvent = suspend {
            delay((WAITING_MILLIS_RANGE.random(random) * ACCELERATION_COEFFICIENT).toLong())
        }
        ExtendedTask(
            priority = priority,
            jobPortion = {
                delay(duration)
                val needWait = random.nextBoolean()
                if (needWait) waitingEvent else null
            }
        ) as T
    }
}

