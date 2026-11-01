package com.taskmanager.service;

import com.taskmanager.executor.TaskExecutor;
import com.taskmanager.model.Task;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.scheduler.TaskScheduler;
import com.taskmanager.storage.TaskStorage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskManager {

    private final Map<String, Task> taskStore = new ConcurrentHashMap<>();
    private final TaskScheduler scheduler = new TaskScheduler();
    private final TaskExecutor executor;
    private final TaskStorage storage;

    public TaskManager(int poolSize) {
        this(poolSize, Path.of("tasks.dat"));
    }

    public TaskManager(int poolSize, Path storageFile) {
        this.executor = new TaskExecutor(poolSize, scheduler);
        this.storage = new TaskStorage(storageFile);
    }

    // FIX 4: only unfinished work is re-queued. COMPLETED / FAILED tasks are kept as history only.
    public void loadPreviousTasks() {
        int requeued = 0;
        for (Task task : storage.loadTasks()) {
            Task.syncSequence(task.getSequence());
            task.resetForRecovery();                  // RUNNING at crash time -> PENDING
            taskStore.put(task.getTaskId(), task);
            if (task.getStatus() == TaskStatus.PENDING) {
                scheduler.addTask(task);
                requeued++;
            }
        }
        System.out.println("Loaded previous tasks, re-queued " + requeued + " unfinished task(s)");
    }

    public void submitTask(Task task) {
        taskStore.put(task.getTaskId(), task);
        scheduler.addTask(task);
    }

    public void start() {
        executor.start();
    }

    public void printTaskStatuses() {
        taskStore.values().stream()
                .sorted(Comparator.comparingLong(Task::getSequence))
                .forEach(t -> System.out.println(t.getTaskName() + " : " + t.getStatus()));
    }

    // FIX 4: wait for work to finish, stop the workers, and ONLY THEN save, so saved statuses are accurate.
    public void shutdown(long timeoutMillis) {
        boolean interrupted = false;
        try {
            if (!scheduler.awaitIdle(timeoutMillis)) {
                System.out.println("Timeout reached: unfinished tasks will be saved and resumed next run");
            }
        } catch (InterruptedException e) {
            interrupted = true;
        }
        try {
            executor.stop(5000);
        } catch (InterruptedException e) {
            interrupted = true;
        }
        storage.saveTasks(new ArrayList<>(taskStore.values()));
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}