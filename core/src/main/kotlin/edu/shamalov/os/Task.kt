package edu.shamalov.os

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import java.util.Objects
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

abstract class Task internal constructor(
    val priority: Priority = Priority.default,
    private val isBasic: Boolean = true
) {
    private val id = Task.id.getAndIncrement()
    protected abstract val jobPortion: suspend () -> Unit

    var state: State = State.Suspended(isBasic)
        protected set

    private var needWaiting = AtomicBoolean(false)

    private lateinit var job: Deferred<Unit>

    suspend fun onEvent(event: Event): State {
        logger.debug { "$this received event $event" }
        state = state.succeededBy(event)

        when {
            event is Event.Wait -> {
                require(this is ExtendedTask)
                coroutineScope { job = async { jobPortion() } }
                job.await()
            }
            event is Event.Preempt -> job.cancel(event.toString())
            event is Event.Start -> {
                coroutineScope { job = async { jobPortion() } }
                job.await()

                logger.debug { "$this finished execution" + if (this is ExtendedTask && needWaiting.get()) ", WAITING for an event" else "" }
                if (needWaiting.get()) {
                    onEvent(Event.Wait)
                } else {
                    onEvent(Event.Terminate)
                }

                require(this is ExtendedTask || state !is ExtendedState.Waiting) { "Basic task can't wait other events" }
                needWaiting.set(false)
            }
        }

        return state
    }

    protected fun needWaiting() {
        require(this is ExtendedTask)
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

    override fun hashCode(): Int = Objects.hash(id, isBasic, priority)

    companion object {
        private val id = AtomicInteger(0)
    }
}

private val logger = KotlinLogging.logger { }
