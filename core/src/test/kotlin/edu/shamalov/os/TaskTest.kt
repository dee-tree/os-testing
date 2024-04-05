package edu.shamalov.os

import io.mockk.every
import io.mockk.spyk
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.time.Duration.Companion.days

class TaskTest {

    @Test
    fun testEquals() {
        val task1 = spyk(BasicTask(Priority.default) {})

        assertEquals(task1, task1)
        assertEquals(task1.hashCode(), task1.hashCode())

        val task2 = spyk(BasicTask(Priority.default) {})
        assertNotEquals(task1, task2)
        assertNotEquals(task1.hashCode(), task2.hashCode())

        val task3 = spyk(BasicTask(Priority.max) {})
        every { task3.id } returns task1.id
        assertNotEquals(task1, task3)
        assertNotEquals(task1.hashCode(), task3.hashCode())

        val task4 = spyk(BasicTask(Priority.default) {})
        every { task4.id } returns task1.id
        every { task4.state } returns State.Running(true)
        assertNotEquals(task1, task4)
        assertNotEquals(task1.hashCode(), task4.hashCode())

        val task5 = spyk(BasicTask(Priority.default) {})
        every { task5.id } answers { task1.id }
        assertEquals(task1, task5)
        assertEquals(task1.hashCode(), task5.hashCode())

        val task6 = spyk(createExtendedTask(Priority.default) {})
        every { task6.id } returns task1.id
        assertNotEquals<Task>(task1, task6)
        assertNotEquals(task1.hashCode(), task6.hashCode())
    }

    @Test
    fun testWaitIllegal() {
        val task = spyk(BasicTask(Priority.default) {})
        assertThrows<RuntimeException> { runTest { task.onEvent(Event.Wait) } }

        every { task.state } returns State.Running(false)
        assertThrows<RuntimeException> { runTest { task.onEvent(Event.Wait) } } // task must have job if it is running
    }

    @Test
    fun testWaitLegal() {
        val task = spyk(createExtendedTask(Priority.default) { })

        every { task.state } returns State.Running(false) andThenAnswer { callOriginal() }
        assertDoesNotThrow { runTest { task.onEvent(Event.Wait) } }
        assertIs<ExtendedState.Waiting>(task.state)
    }

    @Test
    fun testPreemptIllegal() {
        val task = spyk(BasicTask(Priority.default) { delay(1.days) })
        assertThrows<RuntimeException> { runTest { task.onEvent(Event.Preempt) } }

        every { task.state } returns State.Running(true)
        assertThrows<RuntimeException> { runTest { task.onEvent(Event.Preempt) } }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testPreemptLegal() {
        val task = spyk(BasicTask(Priority.default) { delay(1.days) })
        runTest { task.onEvent(Event.Activate) }
        runTest {
            launch(Dispatchers.IO) {
                task.onEvent(Event.Start)
            }
            withContext(Dispatchers.Default.limitedParallelism(1)) {
                delay(100)
            }

            task.onEvent(Event.Preempt)
            assertIs<State.Ready>(task.state)

        }
    }
}