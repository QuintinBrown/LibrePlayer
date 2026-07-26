package com.QuintinBrown.LibrePlayer.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {

        stage.setTitle("LibrePlayer");
        stage.setScene(new Scene(new Label("Test"), 400, 300));
        stage.show();
    }
    
}
