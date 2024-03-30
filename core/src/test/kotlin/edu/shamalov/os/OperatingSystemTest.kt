package edu.shamalov.os

import edu.shamalov.os.schedule.Scheduler
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.measureTime

class OperatingSystemTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun simpleTest() = runTest {
        val scheduler = spyk(Scheduler())
        val processor = mockk<Processor>()
        every { processor.close() } returns Unit

        val os = OperatingSystem(processor, scheduler)
        with(os) { start() }

        withContext(Dispatchers.Default.limitedParallelism(1)) { delay(100) } // startup

        os.stop()

        verify(exactly = 0) { scheduler.hasHigherPriorityTask(Priority.min) }
        coVerify(exactly = 0) { scheduler.offer(any(), any()) }
        coVerify(exactly = 1) { scheduler.pop(any()) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testEnqueue() = runTest {
        val scheduler = spyk(Scheduler())
        val processor = spyk(Processor())

        val os = OperatingSystem(processor, scheduler)
        with(os) { start() }

        listOf(
            BasicTask(Priority.default) { delay(200) },
            createExtendedTask(Priority.default) { delay(200) }
        ).forEach { taskScheme ->
            val task: Task = spyk(taskScheme)

            val capturedEvents = mutableListOf<Event>()
            coEvery { task.onEvent(capture(capturedEvents)) }.coAnswers { callOriginal() }

            assertEquals(0, processor.currentTasksCount)
            os.enqueueTask(task)

            withContext(Dispatchers.Default.limitedParallelism(1)) {
                delay(100)
                assertEquals(1, processor.currentTasksCount)
                delay(200)
                assertEquals(0, processor.currentTasksCount)
            }

            assertTrue { capturedEvents.filterIsInstance<Event.Activate>().isNotEmpty() }
            assertTrue { capturedEvents.filterIsInstance<Event.Start>().isNotEmpty() }

            assertIs<State.Suspended>(task.state)
            verify(exactly = 1) { with(processor) { any<CoroutineScope>().execute(task) } }
            coVerify(exactly = 1) { scheduler.offer(task, any()) }
            clearMocks(processor, verificationMarks = true)
        }

        coVerify(exactly = 3) { scheduler.pop(any()) }
        os.stop()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testWaiting() = runTest {
        val scheduler = spyk(Scheduler())
        val processor = spyk(Processor())

        val os = OperatingSystem(processor, scheduler)
        with(os) { start() }

        val task = run {
            var taskCallTimes = -1
            // firstly wait, secondly complete
            val waitingEvent = suspend { delay(200) }
            val taskScheme = ExtendedTask(
                Priority.default,
                suspend { taskCallTimes++; delay(200); if (taskCallTimes == 0) waitingEvent else null })
            spyk(taskScheme)
        }


        val capturedEvents = mutableListOf<Event>()
        coEvery { task.onEvent(capture(capturedEvents)) }.coAnswers { callOriginal() }

        assertEquals(0, processor.currentTasksCount)
        os.enqueueTask(task)

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            measureTime {
                withTimeout(800) { // 200 + 200 + overhead
                    while (task.state !is ExtendedState.Waiting);
                    assertIs<ExtendedState.Waiting>(task.state)

                    while (task.state !is State.Suspended);
                    assertIs<State.Suspended>(task.state)
                }
            }.also { assertTrue { it.inWholeMilliseconds > 400 } }
        }

        assertIs<Event.Activate>(capturedEvents[0])
        assertIs<Event.Start>(capturedEvents[1])
        assertIs<Event.Release>(capturedEvents[2])
        assertIs<Event.Start>(capturedEvents[3])

        os.stop()
    }
}