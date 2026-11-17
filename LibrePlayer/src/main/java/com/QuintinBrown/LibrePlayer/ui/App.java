package com.QuintinBrown.LibrePlayer.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.QuintinBrown.LibrePlayer.library.AppDirectories;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class App extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    @Override
    public void start(Stage stage) {

        try {

            WebView webView = new WebView();
            WebEngine webEngine = webView.getEngine();

            // TODO: Replace with user selected directory
            webEngine.load(AppDirectories.themeDir().resolve("default/index.html").toString());
            Scene scene = new Scene(webView, 800, 600);

            stage.setScene(scene);
            stage.setTitle("LibrePlayer");
            stage.show();

            LOGGER.info("Started UI");

        } catch (Exception e) {
            LOGGER.error("Error in UI:" + e);
        }
    }
    
}
