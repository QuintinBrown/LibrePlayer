package com.QuintinBrown.LibrePlayer.ui;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.QuintinBrown.LibrePlayer.library.AlbumMetadata;
import com.QuintinBrown.LibrePlayer.library.DatabaseManager;
import com.QuintinBrown.LibrePlayer.library.ImportManager;
import com.QuintinBrown.LibrePlayer.library.TrackMetadata;
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
    
    public LibraryController(Window owner, DatabaseManager database) {
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

    public String getAllAlbums() {
        LOGGER.info("Getting all albums");
        Map<Long, AlbumMetadata> albumMetadataList = database.getAllAlbums();
        LOGGER.info("Retrieved {} albums", albumMetadataList.size());
        JsonArray albums = new JsonArray();

        for (Map.Entry<Long, AlbumMetadata> albumData : albumMetadataList.entrySet()) {
            LOGGER.info("Processing album with ID: {}", albumData.getKey());
            JsonObject album = new JsonObject();
            album.addProperty("id", albumData.getKey());
            album.addProperty("title", albumData.getValue().getName());
            album.addProperty("releaseDate", albumData.getValue().getReleaseDate());
            album.addProperty("artists", albumData.getValue().getAlbumArtists().toString());

            Path coverPath = albumData.getValue().getCoverPath();
            album.addProperty("coverPath", coverPath != null ? coverPath.toString() : null);

            LOGGER.info("Getting track count for album with ID {}", albumData.getKey());
            album.addProperty("trackCount", database.getTrackCount(albumData.getKey()));
            albums.add(album);

            LOGGER.info("Finished processing album {}", albumData.getKey());
        }

        LOGGER.info("Serializing {} albums to JSON", albums.size());

        return albums.toString();
    }

    public String getAlbumTracks(long albumID) {
        Map<Long, TrackMetadata> trackMetadataList = database.getTracksFromAlbum(albumID);
        JsonArray tracks = new JsonArray();

        for (Map.Entry<Long, TrackMetadata> trackData : trackMetadataList.entrySet()) {
            JsonObject track = new JsonObject();
            track.addProperty("id", trackData.getKey());
            track.addProperty("title", trackData.getValue().getTitle());
            track.addProperty("artists", trackData.getValue().getArtists().toString());
            track.addProperty("trackNumber", trackData.getValue().getTrackNumber());
            track.addProperty("duration", trackData.getValue().getLengthSeconds());
            tracks.add(track);
        }

        return tracks.toString();
    }

    public String getTrackMetadata(long trackID) {

        TrackMetadata track = database.getTrack(trackID);

        if (track == null) {
            return null;
        }

        AlbumMetadata album = database.getAlbumFromTrack(trackID);

        JsonObject metadata = new JsonObject();

        metadata.addProperty("title", track.getTitle());
        metadata.addProperty("artist", String.join(", ", track.getArtists()));

        metadata.addProperty("album", album != null ? album.getName() : "Unknown Album");

        metadata.addProperty("releaseDate",album != null ? album.getReleaseDate() : "");

        metadata.addProperty(
                "artworkPath",
                album != null && album.getCoverPath() != null
                        ? album.getCoverPath().toString()
                        : null
        );

        metadata.addProperty("duration", track.getLengthSeconds());

        return metadata.toString();
    }
}
