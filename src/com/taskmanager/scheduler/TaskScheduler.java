package com.taskmanager.scheduler;

import com.taskmanager.model.Task;
import java.util.PriorityQueue;
import java.util.Queue;

public class TaskScheduler {

    private final Queue<Task> taskQueue = new PriorityQueue<>();

    // Add task to queue
    public void addTask(Task task) {
        taskQueue.offer(task);
    }

    // Get next task to execute
    public Task getNextTask() {
        return taskQueue.poll();
    }

    // Check if tasks exist
    public boolean hasTasks() {
        return !taskQueue.isEmpty();
    }
}
