package edu.shamalov.os

import kotlin.random.Random
import kotlinx.coroutines.delay

fun generateTask(random: Random = Random.Default): Task {
    val priority = Priority(PRIORITY_RANGE.random(random))
    val isBasic = random.nextBoolean()

    val delayTime = (RUNNING_MILLIS_RANGE.random(random) * ACCELERATION_COEFFICIENT).toLong()
    return if (isBasic) {
        BasicTask(
            priority = priority,
            jobPortion = {
                delay(timeMillis = delayTime)
            }
        )
    } else {
        val waitingEvent = suspend {
            delay((WAITING_MILLIS_RANGE.random(random) * ACCELERATION_COEFFICIENT).toLong())
        }
        ExtendedTask(
            priority = priority,
            jobPortion = {
                delay(timeMillis = delayTime)
                val needWait = random.nextBoolean()
                if (needWait) waitingEvent else null
            }
        )
    }
}

