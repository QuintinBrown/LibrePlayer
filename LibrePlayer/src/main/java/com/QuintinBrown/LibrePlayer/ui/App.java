package com.QuintinBrown.LibrePlayer.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    @Override
    public void start(Stage stage) {

        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/App.fxml"));

            Parent root = loader.load();

            Scene scene = new Scene(root);

            stage.setScene(scene);
            stage.setTitle("LibrePlayer");
            stage.show();

            LOGGER.info("Started UI");

        } catch (Exception e) {
            LOGGER.error("Error in UI:" + e);
        }
    }
    
}
