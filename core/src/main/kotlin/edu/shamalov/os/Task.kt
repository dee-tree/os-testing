package edu.shamalov.os

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.atomic.AtomicInteger

abstract class Task(private val os: OperatingSystem, private val jobPortion: suspend () -> State) {
    abstract val isBasic: Boolean

    //    abstract val isCompleted: Boolean // TODO: do we need this? Looks like spec do not follow it
    abstract val priority: Priority
    private val id = Task.id.getAndIncrement()

    @Suppress("LeakingThis")
    var state: State = State.Suspended(isBasic)
        protected set

    private lateinit var job: Deferred<State>
    suspend fun onEvent(event: Event): State {
        logger.debug { "$this received event $event}" }
        state = state.succeededBy(event)

        when {
            event is Event.Preempt -> job.cancel(event.toString())
            event is Event.Start -> {
                coroutineScope { job = async { jobPortion() } }
                state = job.await()

                if (state is ExtendedState.Waiting && this.isBasic) throw IllegalStateException("Basic task can't wait other events")
                if (state !is State.Suspended && state !is ExtendedState.Waiting && state !is State.Ready)
                    throw IllegalStateException("Invalid job result: $state")
            }
        }

        return state
    }

    override fun toString(): String = "Task#$id(${if (isBasic) "B" else "E"} : $priority : $state)"

    companion object {
        private val id = AtomicInteger(0)
    }
}

private val logger = KotlinLogging.logger { }