package edu.shamalov.os.schedule

import edu.shamalov.os.Event
import edu.shamalov.os.ExtendedState
import edu.shamalov.os.Priority
import edu.shamalov.os.State
import edu.shamalov.os.Task
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext

class Scheduler(val capacity: UInt = DEFAULT_QUEUE_CAPACITY, private val queue: TasksQueue = TasksQueue()) {
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private val dispatcher = newSingleThreadContext("scheduler")

    /**
     * Provides an ability to limit capacity via suspensions
     */
    private val offerSemaphore = Semaphore(capacity.toInt())
    private val popSemaphore = Semaphore(capacity.toInt(), acquiredPermits = capacity.toInt())

    /**
     * Provides an ability to lock operations with scheduler
     */
    private val mutex = Mutex()
    private var locker: Any? = null

    val isFull: Boolean
        get() = queue.size - capacity.toInt() >= 0

    fun hasHigherPriorityTask(than: Priority): Boolean {
        return if (queue.isEmpty()) false else queue.maxPriority > than
    }

    suspend fun offer(task: Task, locker: Any? = null) = withContext(dispatcher) {
        val reentrant = this@Scheduler.locker != null && locker == this@Scheduler.locker
        if (!reentrant) lock(locker) // acquire lock
        if (task.state !is State.Ready && task.state !is State.Suspended && task.state !is ExtendedState.Waiting)
            throw IllegalArgumentException("Unable to enqueue a task that's not ready nor suspended nor waiting")

        offerSemaphore.acquire() // suspends until has space
        popSemaphore.release()

        when (task.state) {
            is State.Suspended -> task.onEvent(Event.Activate)
            is ExtendedState.Waiting -> task.onEvent(Event.Release)
            else -> Unit
        }
        queue.offer(task)
        logger.debug { "$task is added to the queue | enqueued ${queue.size} tasks" }
        if (!reentrant) mutex.unlock(locker)
    }

    suspend fun pop(locker: Any? = null): Deferred<Task> = withContext(dispatcher) {
        async {
            val reentrant = this@Scheduler.locker != null && locker == this@Scheduler.locker
            if (!reentrant) lock(locker) // acquire lock

            popSemaphore.acquire()
            offerSemaphore.release()

            val task = queue.pop()
            require(task.state is State.Ready) { "actual state: ${task.state} of $task" }
            logger.debug { "$task is popped from the queue | enqueued ${queue.size} tasks" }
            task.also { if (!reentrant) mutex.unlock(locker) }
        }
    }

    suspend fun lock(x: Any? = null) {
        mutex.lock(x)
        locker = x
    }

    fun unlock(x: Any? = null) {
        mutex.unlock(x)
        locker = null
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}

const val DEFAULT_QUEUE_CAPACITY = 10u