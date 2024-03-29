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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class Scheduler(val capacity: UInt = DEFAULT_QUEUE_CAPACITY) {

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private val dispatcher = newSingleThreadContext("Scheduler thread")
    private val queue = TasksQueue()
    private val mutex = Mutex()

    /**
     * We need limited storage this to suspend queue operations in corner cases to not block/throw
     */
    private val limitedStorage = Channel<Unit>(capacity.toInt())

    val isFull: Boolean
        get() = queue.size - capacity.toInt() >= 0

    fun hasHigherPriorityTask(than: Priority): Boolean {
        return if (queue.isEmpty()) false else queue.maxPriority > than
    }

    suspend fun offer(task: Task) = withContext(dispatcher) {
        if (task.state !is State.Ready && task.state !is State.Suspended && task.state !is ExtendedState.Waiting) throw IllegalArgumentException(
            "Unable to enqueue a task that's not ready nor suspended nor waiting"
        )

        limitedStorage.send(Unit) // suspends until has space
        when (task.state) {
            is State.Suspended -> task.onEvent(Event.Activate)
            is ExtendedState.Waiting -> task.onEvent(Event.Release)
            else -> Unit
        }
        queue.offer(task)

        logger.debug { "$task is added to the queue | enqueued ${queue.size} tasks" }
    }

    suspend fun pop(): Deferred<Task> = withContext(dispatcher) {
        async {
            limitedStorage.receive()
            val task = queue.pop()
            require(task.state is State.Ready) { "actual state: ${task.state} of $task" }
            logger.debug { "$task is popped from the queue | enqueued ${queue.size} tasks" }
            task
        }
    }


    suspend fun lock(x: Any) {
        mutex.lock(x)
    }

    fun unlock(x: Any) {
        mutex.unlock(x)
    }

    @OptIn(ExperimentalContracts::class)
    suspend fun <T> withLock(action: Scheduler.() -> T): T {
        contract {
            callsInPlace(action, InvocationKind.EXACTLY_ONCE)
        }
        return mutex.withLock {
            this.action()
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}

const val DEFAULT_QUEUE_CAPACITY = 10u