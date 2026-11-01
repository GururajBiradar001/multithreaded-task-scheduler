package com.taskmanager.service;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskManagerLoadTest {

    @AfterEach
    void restoreLogging() {
        Task.setVerbose(true);
    }

    @Test
    @Timeout(30)
    void fiveHundredTasksFromManyThreadsAllCompleteExactlyOnce(@TempDir Path tempDir) throws Exception {
        Task.setVerbose(false);
        TaskManager manager = new TaskManager(8, tempDir.resolve("tasks.dat"));
        List<Task> all = new CopyOnWriteArrayList<>();

        manager.start();

        ExecutorService submitters = Executors.newFixedThreadPool(4);
        CountDownLatch submitted = new CountDownLatch(4);
        for (int s = 0; s < 4; s++) {
            int id = s;
            submitters.execute(() -> {
                for (int i = 0; i < 125; i++) {
                    Task t = new Task("S" + id + "-T" + i, TaskPriority.values()[i % 3], 10);
                    all.add(t);
                    manager.submitTask(t);
                }
                submitted.countDown();
            });
        }
        submitted.await();
        submitters.shutdown();
        manager.shutdown(20_000);

        assertEquals(500, all.size());
        assertTrue(all.stream().allMatch(t -> t.getStatus() == TaskStatus.COMPLETED));
        assertTrue(all.stream().allMatch(t -> t.getExecutions() == 1));
    }
}