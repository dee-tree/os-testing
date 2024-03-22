package edu.shamalov.os

interface Task {
    val isBasic: Boolean
    val isCompleted: Boolean // TODO: do we need this? Looks like spec do not follow it
    val priority: Priority
    val state: State
    fun onEvent(event: Event)
}
