package edu.shamalov.os.event

sealed class Event(val isBasic: Boolean) {
    data object Activate : Event(true)
    data object Start : Event(true)
    data object Preempt : Event(true)
    data object Terminate : Event(true)

    data object Wait : Event(false)
    data object Release : Event(false)
}