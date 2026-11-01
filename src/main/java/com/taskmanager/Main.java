package com.taskmanager;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.service.TaskManager;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        TaskManager manager = new TaskManager(3);       // 3 worker threads
        manager.loadPreviousTasks();

        // Submit LOW first on purpose: priority, not submission order, must decide who runs first.
        for (int i = 1; i <= 4; i++) {
            manager.submitTask(new Task("CleanTemp-" + i, TaskPriority.LOW, 300));
        }
        for (int i = 1; i <= 3; i++) {
            manager.submitTask(new Task("SendEmail-" + i, TaskPriority.MEDIUM, 300));
        }
        manager.submitTask(new Task("GenReport", TaskPriority.HIGH, 300));

        manager.start();

        // Submitted AFTER workers started: these HIGH tasks must still jump the queue.
        Thread.sleep(100);
        manager.submitTask(new Task("UrgentFix-A", TaskPriority.HIGH, 300));
        manager.submitTask(new Task("UrgentFix-B", TaskPriority.HIGH, 300));

        manager.shutdown(30_000);                        // waits for everything, then saves
        System.out.println("---- final statuses ----");
        manager.printTaskStatuses();
    }
}
