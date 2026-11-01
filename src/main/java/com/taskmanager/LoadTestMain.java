package com.taskmanager;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.service.TaskManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

// Proves the resume claim: 500+ tasks from many threads, every one completed exactly once.
public class LoadTestMain {

    public static void main(String[] args) throws Exception {
        final int submitters = 4;
        final int perSubmitter = 125;                    // 4 x 125 = 500 tasks
        final int workers = 8;
        final long workMillis = 10;

        Task.setVerbose(false);                          // no console spam
        Path tmpFile = Path.of(System.getProperty("java.io.tmpdir"), "loadtest-tasks.dat");
        Files.deleteIfExists(tmpFile);

        TaskManager manager = new TaskManager(workers, tmpFile);
        List<Task> all = new CopyOnWriteArrayList<>();
        TaskPriority[] priorities = TaskPriority.values();

        long begin = System.nanoTime();
        manager.start();

        // 4 threads submit at the same time -> hammers the queue's thread-safety
        ExecutorService submitterPool = Executors.newFixedThreadPool(submitters);
        CountDownLatch done = new CountDownLatch(submitters);
        for (int s = 0; s < submitters; s++) {
            final int id = s;
            submitterPool.execute(() -> {
                for (int i = 0; i < perSubmitter; i++) {
                    TaskPriority p = priorities[ThreadLocalRandom.current().nextInt(priorities.length)];
                    Task t = new Task("S" + id + "-T" + i, p, workMillis);
                    all.add(t);
                    manager.submitTask(t);
                }
                done.countDown();
            });
        }
        done.await();
        submitterPool.shutdown();

        manager.shutdown(60_000);
        long millis = (System.nanoTime() - begin) / 1_000_000;

        long completed = all.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        long exactlyOnce = all.stream().filter(t -> t.getExecutions() == 1).count();

        System.out.println("Submitted        : " + all.size());
        System.out.println("COMPLETED        : " + completed);
        System.out.println("Ran exactly once : " + exactlyOnce);
        System.out.println("Time             : " + millis + " ms with " + workers + " workers");
        System.out.println(completed == all.size() && exactlyOnce == all.size() ? "PASS" : "FAIL");

        Files.deleteIfExists(tmpFile);
    }
}
