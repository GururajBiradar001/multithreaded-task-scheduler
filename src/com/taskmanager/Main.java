package com.taskmanager;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.service.TaskManager;

public class Main {

    public static void main(String[] args) {

        TaskManager manager = new TaskManager(3);

        manager.submitTask(new Task("Generate Report", TaskPriority.HIGH));
        manager.submitTask(new Task("Send Email", TaskPriority.MEDIUM));
        manager.submitTask(new Task("Clean Temp Files", TaskPriority.LOW));

        manager.start();

        try{
            Thread.sleep(1000);
        } catch (InterruptedException e){
            e.printStackTrace();
        }

        manager.printTaskStatuses();
        manager.shutdown();
    }
}