package edu.shamalov.os

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProcessorTest {

    @Test
    fun testInvalidProcessor() {
        assertThrows<RuntimeException> { Processor(0u) }
    }

    @Test
    fun testRunBasic() = runTest {
        val processor = Processor(1u)
        val task1 = BasicTask(Priority(priorityRange.random())) {
            delay(100)
        }.apply { onEvent(Event.Activate) }

        assertEquals(0, processor.currentTasksCount)
        val job = with(processor) { execute(task1) }
        assertEquals(1, processor.currentTasksCount)
        job.await()
        assertEquals(0, processor.currentTasksCount)
        processor.close()
    }

    @Test
    fun testRunIllegalStateBasic() = runTest {
        val processor = Processor(1u)

        // ----- SUSPEND ----- //
        BasicTask(Priority.default) {
            delay(100)
        }.also {
            runCatching {
                coroutineScope { with(processor) { execute(it) }.await() }
            }.also {
                assertTrue { it.isFailure }
            }
            assertEquals(0, processor.currentTasksCount)
        }

        // ----- RUNNING ----- //
        BasicTask(Priority.default) {
            delay(100)
        }.also {
            it.onEvent(Event.Activate)
            val anotherProc = Processor(1u)

            val job = with(processor) { execute(it) }
            delay(10)
            runCatching {
                coroutineScope {
                    with(anotherProc) { execute(it) }.await()
                }
            }.also {
                assertTrue { it.isFailure }
            }
            assertEquals(1, processor.currentTasksCount)
            job.await()
            assertEquals(0, processor.currentTasksCount)
        }
    }

    @Test
    fun testRunIllegalStateExtended() = runTest {
        val processor = Processor(1u)

        // ----- SUSPEND ----- //
        ExtendedTask(Priority.default) {
            delay(100)
            false
        }.also {
            runCatching {
                coroutineScope { with(processor) { execute(it) }.await() }
            }.also {
                assertTrue { it.isFailure }
            }
            assertEquals(0, processor.currentTasksCount)
        }

        // ----- RUNNING ----- //
        ExtendedTask(Priority.default) {
            delay(100)
            false
        }.also {
            it.onEvent(Event.Activate)
            val anotherProc = Processor(1u)

            val job = with(processor) { execute(it) }
            delay(10)
            runCatching {
                coroutineScope {
                    with(anotherProc) { execute(it) }.await()
                }
            }.also {
                assertTrue { it.isFailure }
            }
            assertEquals(1, processor.currentTasksCount)
            job.await()
            assertEquals(0, processor.currentTasksCount)
        }
    }
}