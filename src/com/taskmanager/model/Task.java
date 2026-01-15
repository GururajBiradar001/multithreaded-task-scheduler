package com.taskmanager.model;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.UUID;
import java.time.LocalDateTime;

public class Task implements Runnable, Comparable<Task>, Serializable {
   
    private static final long serialVersionUID = 1L;
    private final String taskId;
    private String taskName;
    private TaskPriority priority;
    // private TaskStatus status;
    private final AtomicReference<TaskStatus> status;
    private LocalDateTime createdAt;

    public Task(String taskName, TaskPriority priority){
        this.taskId = UUID.randomUUID().toString();
        this.taskName = taskName;
        this.priority = priority;
        // this.status = TaskStatus.PENDING;
        this.status = new AtomicReference<>(TaskStatus.PENDING);
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public void run() {
        try{
            status.set(TaskStatus.RUNNING);
            System.out.println("Executing Task: " + taskName);

            Thread.sleep(2000);

            status.set(TaskStatus.COMPLETED);
            System.out.println("Completed Task: " + taskName);
        } catch (Exception e){
            status.set(TaskStatus.FAILED);
            System.out.println("Task Failed: " + taskName);
        }
    }

   @Override
   public int compareTo(Task other) {
     return Integer.compare(
         this.priority.getValue(),
         other.priority.getValue()
     );
    }

    public String getTaskId() {
        return taskId;
    }
    public String getTaskName() {
        return taskName;
    }
    public TaskPriority getPriority() {
        return priority;
    }
    public TaskStatus getStatus() {
        return status.get();
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
}