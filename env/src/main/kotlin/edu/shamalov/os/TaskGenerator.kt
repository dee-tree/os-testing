package edu.shamalov.os

import kotlin.random.Random
import kotlinx.coroutines.delay

fun generateTask(): Task {
    val priority = Priority(Random.nextInt(MIN_PRIORITY, MAX_PRIORITY + 1))
    val isBasic = Random.nextBoolean()
    val delayTime = (Random.nextLong(MIN_DELAY_RUNNING, MAX_DELAY_RUNNING + 1) * ACCELERATION_COEFFICIENT).toLong()
    return if (isBasic) {
        BasicTask(
            priority = priority,
            jobPortion = {
                 delay(timeMillis = delayTime)
            }
        )
    } else {
        ExtendedTask(
            priority = priority,
            jobPortion = {
                delay(timeMillis = delayTime)
                Random.nextBoolean()
            }
        )
    }
}

