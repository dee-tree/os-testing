# Operating system tasks modeling

----

![coverage](.github/badges/jacoco.svg)
![build](https://github.com/dee-tree/os-testing/actions/workflows/build.yaml/badge.svg?event=push&branch=master)

The project presents a model of an operating system capable of managing the execution of multiple processes in real time. This system provides parallel and asynchronous execution of tasks, which is key to ensuring efficient operation of multitasking systems.

The main components of the system include the operating system, the task scheduler, and the model for executing various types of tasks. The system supports two types of tasks: basic and extended.

Basic  tasks are executed until completion or until a switch to a higher priority task occurs. Extended tasks have an additional state WaitEvent, which allows the processor to be freed and reassigned to lower priority tasks without having to complete the current task.

Each task has certain states, such as running, ready, waiting, and suspended. The scheduler handles transitions between these states according to the rules of the system.

The scheduler also manages task priorities, selecting tasks to execute based on their priority and status. Higher priority tasks have higher execution priority. With the same priority, tasks are selected using the FIFO algorithm, where the first added task is executed first.

# Authors

1. **Dmitriy Sokolov** - *5130903 / 00203*
2. **Rostislav Shamaro** - *5130903 / 00203*


