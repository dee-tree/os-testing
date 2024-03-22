package edu.shamalov.os

// TODO: add extended task
class BasicTask(
    override val priority: Priority
) : Task {

    override val isBasic = true

    override var isCompleted: Boolean = false
        private set

    override var state: State = State.Suspended(isBasic)
        private set

    override fun onEvent(event: Event) {
        state = state.succeededBy(event)
    }
}