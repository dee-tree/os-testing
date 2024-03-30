package edu.shamalov.os.schedule

import edu.shamalov.os.BasicTask
import edu.shamalov.os.MAX_PRIORITY
import edu.shamalov.os.MIN_PRIORITY
import edu.shamalov.os.Priority
import edu.shamalov.os.priorityRange
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

        val popped = withContext(Dispatchers.Default.limitedParallelism(1)) { withTimeout(50) { scheduler.pop().await() } }

        assertEquals(task, popped)

        verify(exactly = 1) { queue.offer(task) }
        verify(exactly = 1) { queue.pop() }
    }
}