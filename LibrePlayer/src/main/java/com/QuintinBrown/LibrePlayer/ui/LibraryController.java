package com.QuintinBrown.LibrePlayer.ui;

import java.io.File;
import java.nio.file.Path;

import com.QuintinBrown.LibrePlayer.library.DatabaseManager;
import com.QuintinBrown.LibrePlayer.library.ImportManager;

import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.stage.DirectoryChooser;

public class LibraryController {

    private final ImportManager importManager;

    private Button importButton;
    
    public LibraryController() {
        DatabaseManager database = new DatabaseManager();
        this.importManager = new ImportManager(database);
    }

    private void chooseMusicFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Music Folder");

        File selectedFolder = chooser.showDialog(importButton.getScene().getWindow());

        if (selectedFolder == null) {
            return;
        }

        Path musicDirectory = selectedFolder.toPath();
        
        Task<Void> importTask = new Task<Void>() {
            @Override
            protected Void call() {
                importManager.importLibrary(musicDirectory);
                return null;
            }
        };

        Thread thread = new Thread(importTask);
        thread.setDaemon(true);
        thread.start();
    }
}
