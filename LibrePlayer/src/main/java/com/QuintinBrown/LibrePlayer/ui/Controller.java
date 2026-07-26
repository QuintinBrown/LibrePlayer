package com.QuintinBrown.LibrePlayer.ui;

import java.io.File;

import com.QuintinBrown.LibrePlayer.playback.Player;

import javafx.fxml.FXML;
import javafx.stage.FileChooser;

public class Controller {
    private final Player PLAYER = new Player();

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
}
