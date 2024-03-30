package edu.shamalov.os.schedule

import edu.shamalov.os.Priority
import edu.shamalov.os.Task
import edu.shamalov.os.PRIORITY_RANGE
import java.util.LinkedList

class TasksQueue internal constructor(tasks: Map<Priority, LinkedList<Task>>) {

    private val tasks: MutableMap<Priority, LinkedList<Task>> = tasks.toMutableMap()
    constructor() : this(hashMapOf())

    init {
        for (priority in PRIORITY_RANGE) {
            this.tasks.getOrPut(Priority(priority)) { LinkedList() }
        }
    }

    val size: Int
        get() = tasks.map { (_, anyTasks) -> anyTasks.size }.sum()

    private val hotTasks: LinkedList<Task>
        get() = tasks.filter { (_, anyTasks) -> anyTasks.isNotEmpty() }.maxBy { (priority, _) -> priority }.value

    val maxPriority: Priority
        get() = hotTasks.first.priority

    fun isEmpty(): Boolean {
        return tasks.all { (_, anyTasks) -> anyTasks.isEmpty() }
    }

    /**
     * Get the first task in the queue by priority, remove it from the queue, and return it
     */
    fun pop(): Task {
        return hotTasks.pop()
    }

    /**
     * Add a task to the queue
     */
    fun offer(task: Task) {
        tasks[task.priority]!!.offer(task)
    }
}
