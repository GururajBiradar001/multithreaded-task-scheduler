package com.taskmanager.scheduler;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskPriority;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class TaskSchedulerTest {

    @Test
    void highestPriorityFirst_andTiesAreFifo() throws InterruptedException {
        TaskScheduler scheduler = new TaskScheduler();
        Task low1 = new Task("low1", TaskPriority.LOW, 1);
        Task low2 = new Task("low2", TaskPriority.LOW, 1);
        Task medium = new Task("medium", TaskPriority.MEDIUM, 1);
        Task high = new Task("high", TaskPriority.HIGH, 1);

        scheduler.addTask(low1);
        scheduler.addTask(low2);
        scheduler.addTask(medium);
        scheduler.addTask(high);

        assertSame(high, scheduler.getNextTask());
        assertSame(medium, scheduler.getNextTask());
        assertSame(low1, scheduler.getNextTask());
        assertSame(low2, scheduler.getNextTask());
    }
}