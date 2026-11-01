package com.taskmanager.storage;

import com.taskmanager.model.Task;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class TaskStorage {

    private final Path file;

    public TaskStorage(Path file) {
        this.file = file;
    }

    // FIX 4: write to a temp file first, then atomically swap it in.
    // A crash mid-write can no longer destroy the previous good file.
    public void saveTasks(List<Task> tasks) {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tmp))) {
            out.writeObject(new ArrayList<>(tasks));
        } catch (IOException e) {
            System.err.println("Failed to save tasks: " + e);
            return;
        }
        try {
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Failed to replace " + file + ": " + e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Task> loadTasks() {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            return (List<Task>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            // Never fail silently: say what happened and keep the bad file for inspection.
            System.err.println("Could not read " + file + " (" + e + "). Moving it aside as .corrupt");
            try {
                Files.move(file, file.resolveSibling(file.getFileName() + ".corrupt"),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // nothing more we can do
            }
            return new ArrayList<>();
        }
    }
}
