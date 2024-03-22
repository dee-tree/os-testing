package edu.shamalov.os

const val MIN_PRIORITY = 0
const val MAX_PRIORITY = 3

val priorityRange = MIN_PRIORITY..MAX_PRIORITY

@JvmInline
value class Priority(val priority: Int): Comparable<Priority> {
    init {
        require(priority in priorityRange) {
            "Priority has to be in specified range: $priorityRange"
        }
    }

    override fun compareTo(other: Priority): Int {
        return this.priority - other.priority
    }

}