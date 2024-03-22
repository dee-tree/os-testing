package edu.shamalov.os.schedule

import edu.shamalov.os.Task

interface Scheduler {
    val currentTask: Task?
    val queue: TasksQueue
}