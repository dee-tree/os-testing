package edu.shamalov.os

import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class PriorityTest {
    @Test
    fun simpleTest() {
        PRIORITY_RANGE.forEach { value ->
            val priority = Priority(value)
            assertEquals(priority.priority, value)
            assertEquals(value.toString(), priority.toString())

            val another = Priority(value)
            assertEquals(priority, another)
            assertEquals(priority.hashCode(), another.hashCode())
        }
    }

    @Test
    fun outOfRangeTest() {
        val lower = MIN_PRIORITY - 1
        val upper = MAX_PRIORITY + 1

        assertThrows<RuntimeException> { Priority(lower) }
        assertThrows<RuntimeException> { Priority(upper) }
    }
}