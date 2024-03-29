package edu.shamalov.os

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import java.util.Objects
import java.util.concurrent.atomic.AtomicInteger

abstract class Task internal constructor(
    val priority: Priority = Priority.default,
    private val isBasic: Boolean = true
) {
    private val id = Task.id.getAndIncrement()
    protected abstract val jobPortion: suspend () -> Unit

    var state: State = State.Suspended(isBasic)
        protected set

    private var needWaiting = false

    private lateinit var job: Deferred<Unit>

    suspend fun onEvent(event: Event): State {
        logger.debug { "$this received event $event}" }
        state = state.succeededBy(event)

        when {
            event is Event.Preempt -> job.cancel(event.toString())
            event is Event.Start -> {
                coroutineScope { job = async { jobPortion() } }
                job.await()
                state = if (needWaiting) state.succeededBy(Event.Wait) else state.succeededBy(Event.Terminate)
                needWaiting = false

                logger.debug { "$this finished execution" + if (this is ExtendedTask && needWaiting) ", WAITING for an event" else "" }

                require(this is ExtendedTask || state !is ExtendedState.Waiting) { "Basic task can't wait other events" }
            }
        }

        return state
    }

    protected fun needWaiting() {
        needWaiting = true
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
