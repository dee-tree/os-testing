package edu.shamalov.os

class ExtendedTask(
    override val priority: Priority,
    os: OperatingSystem,
    jobPortion: suspend () -> State
) : Task(os, jobPortion) {

    override val isBasic = false
}