package edu.shamalov.os.schedule

import edu.shamalov.os.BasicTask
import edu.shamalov.os.Priority
import edu.shamalov.os.priorityRange
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TasksQueueTest {

    @Test
    fun simpleTest() {
        val queue = TasksQueue()
        assertEquals(0, queue.size)
        assertTrue { queue.isEmpty() }
        assertThrows<Throwable> { queue.pop() } // pop from empty queue

        val task = BasicTask(Priority.default) {}
        queue.offer(task)
        assertFalse { queue.isEmpty() }
        assertEquals(1, queue.size)
        assertEquals(task.priority, queue.maxPriority)
        assertEquals(task, queue.pop())
        assertTrue { queue.isEmpty() }
        assertEquals(0, queue.size)
    }

    @Test
    fun testPriority() {
        val queue = TasksQueue()
        val lowTask = BasicTask(Priority.min) {}
        val highTask = BasicTask(Priority.max) {}

        queue.offer(lowTask)
        queue.offer(highTask)

        assertEquals(highTask.priority, queue.maxPriority)
        assertNotEquals(highTask.priority, lowTask.priority)

        val poppedFirst = queue.pop()
        assertNotEquals(lowTask, poppedFirst)
        assertEquals(highTask, poppedFirst)
        assertEquals(lowTask.priority, queue.maxPriority)
        assertEquals(lowTask, queue.pop())
        assertEquals(0, queue.size)

        queue.offer(highTask)
        queue.offer(lowTask)

        assertEquals(highTask, queue.pop())
        assertEquals(lowTask, queue.pop())
        assertEquals(0, queue.size)
    }

    @Test
    fun testTimePriority() {
        priorityRange.map { Priority(it) }.forEach { priority ->
            val queue = TasksQueue()
            val aTask = BasicTask(priority) {}
            val bTask = BasicTask(priority) {}

            queue.offer(aTask)
            queue.offer(bTask)
            assertEquals(aTask, queue.pop())
            assertEquals(bTask, queue.pop())

            queue.offer(bTask)
            queue.offer(aTask)
            assertEquals(bTask, queue.pop())
            assertEquals(aTask, queue.pop())
        }
    }
}