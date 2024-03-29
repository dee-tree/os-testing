package edu.shamalov.os

class BasicTask internal constructor(
    override val priority: Priority,
    os: OperatingSystem,
    jobPortion: suspend () -> State
) : Task(os, jobPortion) {

    override val isBasic = true
}