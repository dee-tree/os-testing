package edu.shamalov.os

import edu.shamalov.os.event.Event
import edu.shamalov.os.state.State

// TODO: add extended task
class BasicTask(
    override val priority: Priority,
    override val isBasic: Boolean
) : Task {

    override var isCompleted: Boolean = false
        private set

    override var state: State = State.Suspended
        private set

    override fun onEvent(event: Event) {
        state = state.succeededBy(event) ?: throw Exception("Illegal") // TODO: check this! maybe succeededBy should throw it
    }

// TODO: delete it

//    override fun taskPreempt() {
//        if (stage == Stages.RUNNING) stage = Stages.READY
//    }

//    override fun taskStart() {
//        if (stage == Stages.READY) stage = Stages.RUNNING
//    }
//
//    override fun taskTerminate() {
//        if (stage == Stages.RUNNING) stage = Stages.SUSPENDED
//    }
//
//    override fun taskActivate() {
//        if (stage == Stages.SUSPENDED) stage = Stages.READY
//    }
//
//    override fun taskWait() {
//        if (stage == Stages.RUNNING && isExtended) stage = Stages.WAITING
//    }
//
//    override fun taskRelease() {
//        if (stage == Stages.WAITING && isExtended) stage = Stages.READY
//    }
}