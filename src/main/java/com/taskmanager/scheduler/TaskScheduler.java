package com.taskmanager.scheduler;

import com.taskmanager.model.Task;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskScheduler {

    // FIX 1: thread-safe priority queue. Many threads can offer()/take() at the same time safely.
    private final PriorityBlockingQueue<Task> queue = new PriorityBlockingQueue<>();

    // queued + currently running. When this hits 0 the system is idle.
    private final AtomicInteger outstanding = new AtomicInteger();
    private final Object idleLock = new Object();

    public void addTask(Task task) {
        outstanding.incrementAndGet();   // count first, so we never look "idle" while a task is being added
        queue.offer(task);
    }

    // Blocks (no busy-waiting) until a task is available; always returns the highest-priority one.
    public Task getNextTask() throws InterruptedException {
        return queue.take();
    }

    // Workers call this after finishing (or abandoning) a task.
    public void taskDone() {
        if (outstanding.decrementAndGet() == 0) {
            synchronized (idleLock) {
                idleLock.notifyAll();
            }
        }
    }

    // Wait until every queued and running task has finished, or the timeout expires.
    public boolean awaitIdle(long timeoutMillis) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        synchronized (idleLock) {
            while (outstanding.get() > 0) {
                long remainingMillis = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
                if (remainingMillis <= 0) {
                    return false;
                }
                idleLock.wait(remainingMillis);
            }
            return true;
        }
    }

    public int queuedCount() {
        return queue.size();
    }
}
