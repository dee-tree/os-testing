package edu.shamalov.os

sealed class Action {
    /**
     * Action to enqueue a random task, when type of task is not matter
     */
    data object EnqueueTask : Action()

    /**
     * Action to enqueue a random basic task
     */
    data object EnqueueBasicTask : Action()

    /**
     * Action to enqueue a random extended task
     */
    data object EnqueueExtendedTask : Action()

    /**
     * Action to stop simulation
     */
    data object Stop : Action()
}