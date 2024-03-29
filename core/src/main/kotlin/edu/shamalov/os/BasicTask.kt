package edu.shamalov.os

class BasicTask(
    priority: Priority,
    override val jobPortion: suspend () -> Unit
) : Task(priority) {
    override val isBasic = true
}