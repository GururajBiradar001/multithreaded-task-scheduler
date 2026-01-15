package com.taskmanager.executor;

import com.taskmanager.model.Task;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskExecutor {

    private final ExecutorService executorService;

    public TaskExecutor(int poolSize) {
        this.executorService = Executors.newFixedThreadPool(poolSize);
    }

    public void execute(Task task) {
        executorService.submit(task);
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
