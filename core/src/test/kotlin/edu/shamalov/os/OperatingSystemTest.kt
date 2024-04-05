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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
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
        coVerify(exactly = 0) { scheduler.offer(any()) }
        coVerify(atLeast = 1) { scheduler.pop() }
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
                delay(500)
                assertEquals(0, processor.currentTasksCount)
            }

            assertTrue { capturedEvents.filterIsInstance<Event.Activate>().isNotEmpty() }
            assertTrue { capturedEvents.filterIsInstance<Event.Start>().isNotEmpty() }

            assertIs<State.Suspended>(task.state)
            verify(exactly = 1) { with(processor) { any<CoroutineScope>().execute(task) } }
            coVerify(exactly = 1) { scheduler.offer(task) }
            clearMocks(processor, verificationMarks = true)
        }

        coVerify(atLeast = 3) { scheduler.pop() }
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
        launch(Dispatchers.IO) { delay(100); os.enqueueTask(task) }

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
        assertIs<Event.Wait>(capturedEvents[2])
        assertIs<Event.Release>(capturedEvents[3])
        assertIs<Event.Start>(capturedEvents[4])

        os.stop()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testPreempt() = runTest {
        val scheduler = spyk(Scheduler())
        val processor = spyk(Processor())

        val os = OperatingSystem(processor, scheduler)
        with(os) { start() }

        val longLowPriorityTask = spyk(BasicTask(Priority.min) { delay(1000L) })
        val highPriorityTask = spyk(BasicTask(Priority.max) { delay(100L) })

        val eventsLow = mutableListOf<Event>()
        val eventsHigh = mutableListOf<Event>()
        coEvery { longLowPriorityTask.onEvent(capture(eventsLow)) }.coAnswers { callOriginal() }
        coEvery { highPriorityTask.onEvent(capture(eventsHigh)) }.coAnswers { callOriginal() }

        assertEquals(0, processor.currentTasksCount)
        os.enqueueTask(longLowPriorityTask)

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(2000L) {
                measureTime {
                    while (longLowPriorityTask.state !is State.Running);
                    assertIs<State.Running>(longLowPriorityTask.state)
                }.also { assertTrue { it.inWholeMilliseconds < 1000 } }

                assertEquals(1, processor.currentTasksCount)
                os.enqueueTask(highPriorityTask)

                while (highPriorityTask.state !is State.Running);
                assertIs<State.Running>(highPriorityTask.state)
                assertIs<State.Ready>(longLowPriorityTask.state)

                while (longLowPriorityTask.state !is State.Running);
                assertIs<State.Running>(longLowPriorityTask.state)

                while (processor.currentTasksCount != 0);
                assertIs<State.Suspended>(longLowPriorityTask.state)
                assertIs<State.Suspended>(highPriorityTask.state)
            }
        }

        assertIs<Event.Activate>(eventsLow[0])
        assertIs<Event.Start>(eventsLow[1])
        assertIs<Event.Preempt>(eventsLow[2])
        assertIs<Event.Start>(eventsLow[3])

        assertIs<Event.Activate>(eventsHigh[0])
        assertIs<Event.Start>(eventsHigh[1])

        os.stop()
    }

    @Test
    fun testEquals() {
        val os = OperatingSystem()
        assertEquals(os, os)
        assertEquals(os.hashCode(), os.hashCode())

        val os2 = OperatingSystem()
        assertNotEquals(os, os2)
        assertNotEquals(os.hashCode(), os2.hashCode())

        val os3 = OperatingSystem(Processor(10u), Scheduler(1u))
        assertNotEquals(os, os3)
        assertNotEquals(os.hashCode(), os3.hashCode())

        assertNotEquals(os, Any())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testCloseWhileActive() = runTest {
        val proc = spyk(Processor())
        val os = OperatingSystem(proc)
        with(os) { start() }

        val task = spyk(BasicTask(Priority.default) { delay(1.days) })
        os.enqueueTask(task)

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(3.seconds) {
                while (task.state !is State.Running) delay(5)
                assertEquals(1, proc.currentTasksCount)

                os.close()
                while (proc.currentTasksCount != 0) delay(5)
                assertEquals(0, proc.currentTasksCount)
            }
        }

    }

    @Test
    fun testString() {
        val os = OperatingSystem()
        assertContains(os.toString(), "Operating System")
        os.close()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testBalance() = runTest {
        val os = OperatingSystem()

        launch(Dispatchers.IO) {
            with(os) { start() }
        }

        val tasks = mutableListOf<Task>()

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            repeat(10) {
                val running = (10..100).random().milliseconds
                val priority = Priority(PRIORITY_RANGE.random())
                val task = if (Random.nextBoolean()) BasicTask(priority) { delay(running) }
                else ExtendedTask(priority) { if (Random.nextBoolean()) null else suspend { delay(running) } }
                tasks += task

                delay(running * 3)
                os.enqueueTask(task)
            }
        }

        withContext(Dispatchers.Default.limitedParallelism(1)) { delay(100) }
        while (tasks.any { it.state !is State.Suspended });
        os.close()
    }
}