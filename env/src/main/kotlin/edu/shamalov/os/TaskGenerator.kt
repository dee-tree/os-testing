package edu.shamalov.os

import kotlin.random.Random
import kotlinx.coroutines.delay

fun generateTask(random: Random = Random.Default): Task {
    val priority = Priority(random.nextInt(MIN_PRIORITY, MAX_PRIORITY + 1))
    val isBasic = random.nextBoolean()
    val delayTime = (random.nextLong(MIN_DELAY_RUNNING, MAX_DELAY_RUNNING + 1) * ACCELERATION_COEFFICIENT).toLong()
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
                random.nextBoolean()
            }
        )
    }
}

