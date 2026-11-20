package com.QuintinBrown.LibrePlayer.ui;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.QuintinBrown.LibrePlayer.library.AlbumMetadata;
import com.QuintinBrown.LibrePlayer.library.DatabaseManager;
import com.QuintinBrown.LibrePlayer.library.ImportManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javafx.concurrent.Task;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

public class LibraryController {

    private final ImportManager importManager;
    private final Window owner;
    private final DatabaseManager database;

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryController.class);
    
    public LibraryController(Window owner) {
        DatabaseManager database = new DatabaseManager();
        this.database = database;
        this.importManager = new ImportManager(database);
        this.owner = owner;
    }

    public void chooseMusicFolder() {
        LOGGER.info("Opening directory chooser");
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Music Folder");

        File selectedFolder = chooser.showDialog(owner);

        LOGGER.info("Directory chooser returned");

        if (selectedFolder == null) {
            LOGGER.info("No folder selected");
            return;
        }

        LOGGER.info("Selected folder: " + selectedFolder);

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

    public String getAlbums() {
        List<AlbumMetadata> albumMetadataList = database.getAllAlbums();
        JsonArray albums = new JsonArray();

        for (AlbumMetadata albumMetadata : albumMetadataList) {
            JsonObject album = new JsonObject();
            album.addProperty("title", albumMetadata.getName());
            album.addProperty("releaseDate", albumMetadata.getReleaseDate());
            album.addProperty("artists", albumMetadata.getAlbumArtists().toString());
            album.addProperty("coverPath", albumMetadata.getCoverPath().toString());
            albums.add(album);
        }

        return albums.toString();
    }
}
