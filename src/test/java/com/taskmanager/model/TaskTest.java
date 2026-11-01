package com.taskmanager.model;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskTest {

    @Test
    void sameTaskRunByManyThreadsExecutesOnlyOnce() throws Exception {
        Task task = new Task("shared", TaskPriority.HIGH, 50);
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            pool.execute(() -> {
                try {
                    startGun.await();          // wait at the gate
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finished.countDown();
                }
            });
        }

        startGun.countDown();                  // open the gate: all 20 threads race
        finished.await();
        pool.shutdown();

        // TODO 1: assert that task.getExecutions() equals 1
        assertEquals(1, task.getExecutions());
        assertEquals(TaskStatus.COMPLETED, task.getStatus());
        // TODO 2: assert that task.getStatus() equals TaskStatus.COMPLETED
    }
}