package edu.shamalov.os

sealed interface State {
    val isBasic: Boolean

    fun succeededBy(atEvent: Event): State = throw IllegalArgumentException("Unacceptable event $atEvent for the state $this")

    data class Suspended(override val isBasic: Boolean) : State, ExtendedState {
        override fun succeededBy(atEvent: Event) = when (atEvent) {
            Event.Activate -> Ready(isBasic)
            else -> super<State>.succeededBy(atEvent)
        }
    }

    data class Ready(override val isBasic: Boolean) : State, ExtendedState {
        override fun succeededBy(atEvent: Event) = when (atEvent) {
            Event.Start -> Running(isBasic)
            else -> super<State>.succeededBy(atEvent)
        }
    }

    data class Running(override val isBasic: Boolean) : State, ExtendedState {
        override fun succeededBy(atEvent: Event) = when {
            atEvent is Event.Preempt -> Ready(isBasic)
            atEvent is Event.Terminate -> Suspended(isBasic)
            !isBasic && atEvent is Event.Wait -> ExtendedState.Waiting
            else -> super<State>.succeededBy(atEvent)
        }
    }

}

sealed interface ExtendedState : State {
    data object Waiting : ExtendedState {
        override val isBasic = false

        override fun succeededBy(atEvent: Event) = when (atEvent) {
            Event.Release -> State.Ready(false)
            else -> super.succeededBy(atEvent)
        }
    }

}