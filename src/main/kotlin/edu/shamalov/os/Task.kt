package edu.shamalov.os
private const val MIN_PRIORITY = 0
private const val MAX_PRIORITY = 3

class Task(
    private val priority: Int,
    private var stage: Stages,
    private val isExtended: Boolean
) : TaskInterface {

    init {
        require(priority in MIN_PRIORITY..MAX_PRIORITY) { "The priority must be between $MIN_PRIORITY and $MAX_PRIORITY" }
    }

    override fun taskPreempt() {
        if (stage == Stages.RUNNING) stage = Stages.READY
    }

    override fun taskStart() {
        if (stage == Stages.READY) stage = Stages.RUNNING
    }

    override fun taskTerminate() {
        if (stage == Stages.RUNNING) stage = Stages.SUSPENDED
    }

    override fun taskActivate() {
        if (stage == Stages.SUSPENDED) stage = Stages.READY
    }

    override fun taskWait() {
        if (stage == Stages.RUNNING && isExtended) stage = Stages.WAITING
    }

    override fun taskRelease() {
        if (stage == Stages.WAITING && isExtended) stage = Stages.READY
    }
}