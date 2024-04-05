package edu.shamalov.os

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import java.util.Objects
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

abstract class Task internal constructor(
    val priority: Priority = Priority.default,
    private val isBasic: Boolean = true
) {
    protected abstract val jobPortion: suspend () -> Unit

    private val _id: Int = Task.id.getAndIncrement()

    val id: Int
        get() = _id

    private var _state: State = State.Suspended(isBasic)

    var state: State
        get() = _state
        protected set(value) {
            _state = value
        }


    private var needWaiting = AtomicBoolean(false)

    private lateinit var job: Deferred<Unit>

    suspend fun onEvent(event: Event): State {
        logger.info { "$this received event $event" }

        when (event) {
            is Event.Wait -> {
                require(this is ExtendedTask)
                state = state.succeededBy(event)
                withContext(Dispatchers.Unconfined) { job = async { jobPortion() } }
                job.await()
            }

            is Event.Preempt -> {
                if (!job.isCompleted) {
                    state = state.succeededBy(event)
                    job.cancel(event.toString())
                }
            }

            is Event.Start -> {
                state = state.succeededBy(event)
                withContext(Dispatchers.Unconfined) {
                    job = async {
                        jobPortion()
                        val needWait = needWaiting.get()
                        needWaiting.set(false)
                        if (needWait) this.cancel(Event.Wait.toString())
                    }
                }

                job.await()
                return onEvent(Event.Terminate)
            }

            else -> state = state.succeededBy(event)
        }

        return state
    }

    protected fun needWaiting() {
        needWaiting.set(true)
    }

    override fun toString(): String = "Task#$id($priority, $state)"

    override fun equals(other: Any?): Boolean {
        if (other !is Task) return false
        return when {
            this.id != other.id -> false
            this.isBasic != other.isBasic -> false
            this.priority != other.priority -> false
            this.state != other.state -> false
            else -> true
        }
    }

    override fun hashCode(): Int = Objects.hash(id, isBasic, priority, state)

    companion object {
        private val id = AtomicInteger(0)
        private val logger = KotlinLogging.logger("Task")
    }
}
