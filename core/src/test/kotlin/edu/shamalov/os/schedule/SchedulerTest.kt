package edu.shamalov.os.schedule

import edu.shamalov.os.BasicTask
import edu.shamalov.os.ExtendedState
import edu.shamalov.os.MAX_PRIORITY
import edu.shamalov.os.MIN_PRIORITY
import edu.shamalov.os.Priority
import edu.shamalov.os.State
import edu.shamalov.os.Task
import edu.shamalov.os.priorityRange
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SchedulerTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun simpleTest() = runTest {
        val queue = spyk<TasksQueue>()

        val scheduler = Scheduler(10u, queue)
        assertFalse { scheduler.isFull }
        assertEquals(10u, scheduler.capacity)
        priorityRange.map { Priority(it) }.forEach { priority ->
            assertFalse { scheduler.hasHigherPriorityTask(priority) }
        }

        val task = BasicTask(Priority.default) {}
        scheduler.offer(task)
        (MIN_PRIORITY..<task.priority.priority).map { Priority(it) }.forEach { priority ->
            assertTrue { scheduler.hasHigherPriorityTask(priority) }
        }

        (task.priority.priority..MAX_PRIORITY).map { Priority(it) }.forEach { priority ->
            assertFalse { scheduler.hasHigherPriorityTask(priority) }
        }

        val popped =
            withContext(Dispatchers.Default.limitedParallelism(1)) { withTimeout(50) { scheduler.pop().await() } }

        assertEquals(task, popped)
        scheduler.close()

        verify(exactly = 1) { queue.offer(task) }
        verify(exactly = 1) { queue.offer(any()) }
        verify(exactly = 1) { queue.pop() }
    }

    @Test
    fun testPopSuspension() = runTest {
        val queue = spyk<TasksQueue>()
        every { queue.pop() } answers { throw RuntimeException() }
        every { queue.offer(any()) } answers { throw RuntimeException() }

        val scheduler = Scheduler(10u, queue)

        runCatching {
            withTimeout(5000) {
                scheduler.pop().await()
                assertTrue("should not be called") { false }
            }
            assertTrue("should not be called") { false }
        }.also {
            assertTrue { it.isFailure }
            assertIs<TimeoutCancellationException>(it.exceptionOrNull())
        }

        verify { queue wasNot called }
    }

    @Test
    fun testOfferSuspension() = runTest {
        val queue = spyk<TasksQueue>()
        val scheduler = Scheduler(1u, queue)

        scheduler.offer(BasicTask(Priority.default) {})
        assertTrue { scheduler.isFull }

        runCatching {
            withTimeout(5000) {
                scheduler.offer(BasicTask(Priority.max) {})
                assertTrue("should not be called") { false }
            }
            assertTrue("should not be called") { false }
        }.also {
            assertTrue { it.isFailure }
            assertIs<TimeoutCancellationException>(it.exceptionOrNull())
        }

        verify(exactly = 1) { queue.offer(any()) }
        verify(exactly = 0) { queue.pop() }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testOfferIllegal(isBasicTask: Boolean) = runTest {
        val scheduler = Scheduler()

        val runningTask = mockk<Task>()
        every { runningTask.state } returns State.Running(isBasicTask)

        runCatching {
            scheduler.offer(runningTask)
        }.also {
            assertTrue { it.isFailure }
            assertIs<RuntimeException>(it.exceptionOrNull())
            assertContains(it.exceptionOrNull()!!.message!!, "Unable to enqueue")
        }
    }

    @Test
    fun testOfferWaiting() = runTest {
        val scheduler = Scheduler()

        val task = mockk<Task>(relaxed = true)
        every { task.state } returns ExtendedState.Waiting

        scheduler.offer(task)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testPopRunning(isBasicTask: Boolean) = runTest {
        val queue = spyk<TasksQueue>()
        val badTask = mockk<Task>()
        every { badTask.state } returns State.Running(isBasicTask)
        every { queue.pop() } returns badTask

        val scheduler = Scheduler(queue = queue)
        scheduler.offer(BasicTask(Priority.default) {}) // fictive task to update semaphore

        runCatching {
            scheduler.pop().await()
        }.also {
            assertTrue { it.isFailure }
            assertIs<RuntimeException>(it.exceptionOrNull())
            assertContains(it.exceptionOrNull()!!.message!!, "broken")
        }
    }
}