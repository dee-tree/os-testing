package edu.shamalov.os

import edu.shamalov.os.schedule.Scheduler

interface OperatingSystem {
    val scheduler: Scheduler
    fun Task.sendEvent(event: Event)
}
