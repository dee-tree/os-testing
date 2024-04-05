package edu.shamalov.os.schedule

import edu.shamalov.os.Event
import edu.shamalov.os.ExtendedState
import edu.shamalov.os.Priority
import edu.shamalov.os.State
import edu.shamalov.os.Task
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class Scheduler(val capacity: UInt = DEFAULT_QUEUE_CAPACITY, private val queue: TasksQueue = TasksQueue()) :
    AutoCloseable {
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private val dispatcher = newSingleThreadContext("scheduler")
    private val channel = Channel<Unit>(capacity.toInt())

    /**
     * Provides an ability to lock operations with scheduler
     */
    private val mutex = Mutex()

    val isFull: Boolean
        get() = queue.size - capacity.toInt() >= 0

    fun hasHigherPriorityTask(than: Priority): Boolean {
        return if (queue.isEmpty()) false else queue.maxPriority > than
    }

    suspend fun offer(task: Task) {//= withContext(dispatcher) {
        if (task.state !is State.Ready && task.state !is State.Suspended && task.state !is ExtendedState.Waiting)
            throw IllegalArgumentException("Unable to enqueue a task that's not ready, not suspended, not waiting: $task")

        when (task.state) {
            is State.Suspended -> task.onEvent(Event.Activate)
            is ExtendedState.Waiting -> task.onEvent(Event.Release)
            else -> Unit
        }

        channel.send(Unit)
        withContext(dispatcher) {
            queue.offer(task)
            logger.debug { "$task is added to the queue | enqueued ${queue.size} tasks" }
        }
    }

    suspend fun pop(): Task {
        channel.receive()
        val task = withContext(dispatcher) {
            queue.pop().also { task ->
                logger.debug { "$task is popped from the queue | enqueued ${queue.size} tasks" }
            }
        }
        require(task.state is State.Ready) { "Queue is broken! Expected queue containing only ready tasks, but got $task" }
        return task
    }

    @OptIn(ExperimentalContracts::class)
    suspend fun <T> withLock(action: suspend Scheduler.() -> T): T {
        contract { callsInPlace(action) }
        return mutex.withLock {
            this.action()
        }
    }

    override fun close() {
        dispatcher.close()
        channel.close()
    }

    companion object {
        private val logger = KotlinLogging.logger("Scheduler")
        const val DEFAULT_QUEUE_CAPACITY = 10u
    }
}
