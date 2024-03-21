package edu.shamalov.os.state

import edu.shamalov.os.event.Event

sealed interface State {

    // TODO: Add succeededBy override for each state
    fun succeededBy(atEvent: Event): State? = null

    data object Suspended : State, ExtendedState
    data object Ready : State, ExtendedState
    data object Running : State, ExtendedState {
        override fun succeededBy(atEvent: Event) = when (atEvent) {
            Event.Preempt -> Ready
            Event.Terminate -> Suspended
            else -> super<State>.succeededBy(atEvent)
        }
    }

}

sealed interface ExtendedState : State {
    data object Waiting : ExtendedState

}
