package edu.shamalov.os.schedule

import edu.shamalov.os.Priority
import edu.shamalov.os.Task
import edu.shamalov.os.priorityRange
import java.util.LinkedList

class TasksQueue {
    private val tasks: MutableMap<Priority, LinkedList<Task>> = hashMapOf()

    init {
        for (priority in priorityRange) {
            tasks[Priority(priority)] = LinkedList()
        }
    }

    private val hotTasks: LinkedList<Task>
        get() = tasks.filter { (_, anyTasks) -> anyTasks.isNotEmpty() }.maxBy { (priority, _) -> priority }.value

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
