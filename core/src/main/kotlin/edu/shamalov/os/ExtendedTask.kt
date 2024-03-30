package edu.shamalov.os

class ExtendedTask(
    priority: Priority,
    /**
     * job portion, which returns true is waiting for some event is required
     */
    jobPortion: suspend () -> (suspend () -> Unit)?
) : Task(priority, false) {
    private var waitEvent: (suspend () -> Unit)? = null

    override val jobPortion: suspend () -> Unit = {
        waitEvent?.let {
            it()
            waitEvent = null
        } ?: run {
            waitEvent = jobPortion()
            waitEvent?.let { needWaiting() }
        }
    }

    override fun toString(): String = "Extended${super.toString()}"
}

fun createExtendedTask(priority: Priority, jobPortion: suspend () -> Unit) = ExtendedTask(
    priority,
    suspend { jobPortion(); null }
)

fun createExtendedTaskWaiting(priority: Priority, jobPortion: suspend () -> (suspend () -> Unit)) = ExtendedTask(
    priority,
    jobPortion
)