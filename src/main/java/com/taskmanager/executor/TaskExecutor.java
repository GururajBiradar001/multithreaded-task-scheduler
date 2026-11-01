package com.taskmanager.executor;

import com.taskmanager.model.Task;
import com.taskmanager.scheduler.TaskScheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskExecutor {

    private final int poolSize;
    private final TaskScheduler scheduler;
    private final ExecutorService pool;
    private final AtomicBoolean started = new AtomicBoolean(false);

    public TaskExecutor(int poolSize, TaskScheduler scheduler) {
        this.poolSize = poolSize;
        this.scheduler = scheduler;

        // Named threads make console output and thread dumps readable: worker-1, worker-2, ...
        ThreadFactory namedFactory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "worker-" + counter.getAndIncrement());
            }
        };
        this.pool = Executors.newFixedThreadPool(poolSize, namedFactory);
    }

    // FIX 2: start N long-lived workers. Each one keeps pulling the best task straight from the
    // priority queue, so a HIGH task submitted later still jumps ahead of waiting LOW tasks.
    public void start() {
        if (!started.compareAndSet(false, true)) {
            return; // already started
        }
        for (int i = 0; i < poolSize; i++) {
            pool.execute(this::workerLoop);
        }
    }

    private void workerLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            Task task;
            try {
                task = scheduler.getNextTask();            // blocks while the queue is empty
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;                                     // shutdown requested
            }
            try {
                task.run();
            } finally {
                scheduler.taskDone();
            }
        }
    }

    // Interrupts idle workers (blocked in take()) and waits for the threads to exit.
    public void stop(long timeoutMillis) throws InterruptedException {
        pool.shutdownNow();
        if (!pool.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS)) {
            System.err.println("Workers did not stop within " + timeoutMillis + " ms");
        }
    }
}
