package edu.shamalov.os

const val MIN_PRIORITY = 0
const val MAX_PRIORITY = 3

val PRIORITY_RANGE = MIN_PRIORITY..MAX_PRIORITY

@JvmInline
value class Priority(val priority: Int): Comparable<Priority> {
    init {
        require(priority in PRIORITY_RANGE) {
            "Priority has to be in specified range: $PRIORITY_RANGE"
        }
    }

    override fun compareTo(other: Priority): Int {
        return this.priority - other.priority
    }

    companion object {
        val max: Priority
            get() = Priority(MAX_PRIORITY)

        val min: Priority
            get() = Priority(MIN_PRIORITY)

        val default: Priority
            get() = Priority((MAX_PRIORITY + MIN_PRIORITY) / 2)
    }

    override fun toString(): String = priority.toString()

}