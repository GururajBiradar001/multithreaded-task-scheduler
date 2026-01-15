package com.taskmanager.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.taskmanager.storage.TaskStorage;
import java.util.List;

import com.taskmanager.executor.TaskExecutor;
import com.taskmanager.model.Task;
import com.taskmanager.scheduler.TaskScheduler;

public class TaskManager {

    private final Map<String, Task> taskStore = new ConcurrentHashMap<>();

    private final TaskScheduler scheduler;
    private final TaskExecutor executor;
    
    public void loadPreviousTasks() {
        List<Task> tasks = TaskStorage.loadTasks();
        for (Task task : tasks) {
            taskStore.put(task.getTaskId(), task);
            scheduler.addTask(task);
        }
    }

    public TaskManager(int poolSize) {
        this.scheduler = new TaskScheduler();
        this.executor = new TaskExecutor(poolSize);
    }

    public void submitTask(Task task) {
        taskStore.put(task.getTaskId(), task);
        scheduler.addTask(task);
    }

    public void printTaskStatuses() {
        taskStore.values().forEach(task ->
            System.out.println(task.getTaskName() + " : " + task.getStatus())
        );
    }

    public void start() {
        while (scheduler.hasTasks()) {
            Task task = scheduler.getNextTask();
            executor.execute(task);
        }
    }

    public void shutdown() {
        TaskStorage.saveTasks(
            taskStore.values().stream().toList()
        );
        executor.shutdown();
    }
}