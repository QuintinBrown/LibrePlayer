package com.QuintinBrown.LibrePlayer.ui;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import com.QuintinBrown.LibrePlayer.library.AlbumMetadata;
import com.QuintinBrown.LibrePlayer.library.DatabaseManager;
import com.QuintinBrown.LibrePlayer.library.ImportManager;
import com.QuintinBrown.LibrePlayer.playback.PlaybackState;
import com.QuintinBrown.LibrePlayer.playback.Player;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

public class Controller {

    private final Player PLAYER;
    private final ImportManager importManager;
    private final DatabaseManager database = new DatabaseManager();

    public Controller() {
        
        this.PLAYER = new Player(database);
        this.importManager = new ImportManager(database);
    }

    public void openFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Audio File");

        File file = chooser.showOpenDialog(null);

        if (file != null) {
            PLAYER.open(file.toPath());
        }
    }

    public void play() {
        PLAYER.play();
    }

    public void pause() {
        PLAYER.pause();
    }

    public void togglePlayPause() {
        if (PLAYER.getState() == PlaybackState.PLAYING) {
            PLAYER.pause();
        } else {
            PLAYER.play();
        }
    }

    public String importMusic() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Music Folder");

        File folder = chooser.showDialog(null);

        if (folder != null) {
            importManager.importLibrary(folder.toPath());
        } else {
            return null;
        }

        return folder.getAbsolutePath();
    }

    public void shutdown() {
        PLAYER.close();
    }

    public Player getPlayer() {
        return PLAYER;
    }
}