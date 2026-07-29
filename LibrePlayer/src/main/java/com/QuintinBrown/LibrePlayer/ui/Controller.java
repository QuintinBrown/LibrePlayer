package com.QuintinBrown.LibrePlayer.ui;

import java.io.File;

import com.QuintinBrown.LibrePlayer.library.DatabaseManager;
import com.QuintinBrown.LibrePlayer.library.ImportManager;
import com.QuintinBrown.LibrePlayer.playback.Player;

import javafx.fxml.FXML;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

public class Controller {
    private final Player PLAYER;
    private final ImportManager importManager;

     public Controller() {
        DatabaseManager database = new DatabaseManager();

        this.PLAYER = new Player();
        this.importManager = new ImportManager(database);
    }


    @FXML
    private void onOpen() {
        FileChooser chooser = new FileChooser();

        File file = chooser.showOpenDialog(null);

        if (file != null) {
            PLAYER.open(file.toPath());
        }
    }

    @FXML
    private void onPlay() {
        PLAYER.play();
    }

    @FXML
    private void onPause() {
        PLAYER.pause();
    }


    @FXML
    private void onImportMusic() {

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Music Folder");

        File folder = chooser.showDialog(null);

        if (folder != null) {
            importManager.importLibrary(folder.toPath());
        }
    }
}
