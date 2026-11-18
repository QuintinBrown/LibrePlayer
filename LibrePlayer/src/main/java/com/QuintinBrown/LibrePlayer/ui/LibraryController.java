package com.QuintinBrown.LibrePlayer.ui;

import java.io.File;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.QuintinBrown.LibrePlayer.library.DatabaseManager;
import com.QuintinBrown.LibrePlayer.library.ImportManager;

import javafx.concurrent.Task;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

public class LibraryController {

    private final ImportManager importManager;
    private final Window owner;

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryController.class);
    
    public LibraryController(Window owner) {
        DatabaseManager database = new DatabaseManager();
        this.importManager = new ImportManager(database);
        this.owner = owner;
    }

    public void chooseMusicFolder() {
        System.out.println("Opening directory chooser");
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Music Folder");

        File selectedFolder = chooser.showDialog(owner);

        System.out.println("Directory chooser returned");

        if (selectedFolder == null) {
            System.out.println("No folder selected");
            return;
        }

        System.out.println("Selected folder: " + selectedFolder);

        Path musicDirectory = selectedFolder.toPath();
        
        Task<Void> importTask = new Task<>() {
            @Override
            protected Void call() {
                importManager.importLibrary(musicDirectory);
                return null;
            }

            @Override
            protected void succeeded() {
                LOGGER.info("Music library import completed");
            }

            @Override 
            protected void failed() {
                LOGGER.error("Music library import failed", getException());
            }
        };

        Thread thread = new Thread(importTask, "Library import thread");
        thread.setDaemon(true);
        thread.start();
    }
}
