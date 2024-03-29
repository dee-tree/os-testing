package edu.shamalov.os

class ExtendedTask(
    priority: Priority,
    /**
     * job portion, which returns true is waiting for some event is required
     */
    jobPortion: suspend () -> Boolean
) : Task(priority, false) {

    override val jobPortion: suspend () -> Unit = {
        if (jobPortion()) {
            needWaiting()
        }
    }

    override fun toString(): String = "Extended${super.toString()}"
}