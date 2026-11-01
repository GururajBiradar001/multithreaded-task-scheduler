package com.taskmanager.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Task implements Runnable, Comparable<Task>, Serializable {

    private static final long serialVersionUID = 3L;

    // Turn off the START/DONE console lines (used by the load test)
    private static volatile boolean verbose = true;

    public static void setVerbose(boolean value) {
        verbose = value;
    }

    // Strictly increasing counter: gives every task a unique, ordered number (a clock can give two tasks the same timestamp).
    private static final AtomicLong SEQUENCE = new AtomicLong();

    // After loading saved tasks, make sure new tasks get numbers higher than the loaded ones.
    public static void syncSequence(long seen) {
        SEQUENCE.accumulateAndGet(seen, Math::max);
    }

    private final long sequence = SEQUENCE.incrementAndGet();
    private final String taskId;
    private final String taskName;
    private final TaskPriority priority;
    private final long workMillis;                 // simulated work duration
    private final LocalDateTime createdAt;
    private final AtomicReference<TaskStatus> status = new AtomicReference<>(TaskStatus.PENDING);
    private final AtomicInteger executions = new AtomicInteger(); // how many times run() really started

    public Task(String taskName, TaskPriority priority) {
        this(taskName, priority, 1000);
    }

    public Task(String taskName, TaskPriority priority, long workMillis) {
        this.taskId = UUID.randomUUID().toString();
        this.taskName = taskName;
        this.priority = priority;
        this.workMillis = workMillis;
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public void run() {
        // FIX 3: compareAndSet is an atomic "check-then-act".
        // Only ONE thread can ever move PENDING -> RUNNING, so a task can never run twice.
        if (!status.compareAndSet(TaskStatus.PENDING, TaskStatus.RUNNING)) {
            log("SKIPPED (already " + status.get() + ")");
            return;
        }
//        status.set(TaskStatus.RUNNING);
        executions.incrementAndGet();
        log("START");

        try {
            Thread.sleep(workMillis);              // <-- your real work would go here
            status.set(TaskStatus.COMPLETED);
            log("DONE");
        } catch (InterruptedException e) {
            // Shutdown interrupted us: put the task back to PENDING so it is retried next start,
            // and restore the interrupt flag so the worker thread knows it must stop.
            status.set(TaskStatus.PENDING);
            Thread.currentThread().interrupt();
            log("INTERRUPTED (will retry on next start)");
        } catch (Exception e) {
            status.set(TaskStatus.FAILED);
            log("FAILED: " + e);
        }
    }

    // After a crash, a task saved as RUNNING never finished, so make it runnable again.
    public void resetForRecovery() {
        status.compareAndSet(TaskStatus.RUNNING, TaskStatus.PENDING);
    }

    // FIX 5: same priority -> lower sequence number first, so it is strictly first-come, first-served.
    @Override
    public int compareTo(Task other) {
        int byPriority = Integer.compare(this.priority.getValue(), other.priority.getValue());
        if (byPriority != 0) return byPriority;
        int bySequence = Long.compare(this.sequence, other.sequence);
        if (bySequence != 0) return bySequence;
        return this.taskId.compareTo(other.taskId);   // only reachable for tasks from different runs
    }

    private void log(String message) {
        if (verbose) {
            System.out.printf("[%s] %-22s %s%n",
                    Thread.currentThread().getName(), priority + "/" + taskName, message);
        }
    }

    public long getSequence() { return sequence; }
    public String getTaskId() { return taskId; }
    public String getTaskName() { return taskName; }
    public TaskPriority getPriority() { return priority; }
    public TaskStatus getStatus() { return status.get(); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public int getExecutions() { return executions.get(); }
}