package com.QuintinBrown.LibrePlayer.ui;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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
        Map<Long, AlbumMetadata> albumMetadataList = database.getAllAlbums();
        JsonArray albums = new JsonArray();

        for (Map.Entry<Long, AlbumMetadata> albumData : albumMetadataList.entrySet()) {
            JsonObject album = new JsonObject();
            album.addProperty("id", albumData.getKey());
            album.addProperty("title", albumData.getValue().getName());
            album.addProperty("releaseDate", albumData.getValue().getReleaseDate());
            album.addProperty("artists", albumData.getValue().getAlbumArtists().toString());
            album.addProperty("coverPath", albumData.getValue().getCoverPath().toString());
            album.addProperty("trackCount", database.getTrackCount(albumData.getKey()));
            albums.add(album);
        }

        return albums.toString();
    }
}
