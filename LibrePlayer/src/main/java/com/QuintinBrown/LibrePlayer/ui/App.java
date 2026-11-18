
package com.QuintinBrown.LibrePlayer.ui;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.QuintinBrown.LibrePlayer.library.AppDirectories;

import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class App extends Application {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(App.class);

    @Override
    public void start(Stage stage) {

        try {

            WebView webView = new WebView();
            WebEngine webEngine = webView.getEngine();

            // Get theme HTML file
            Path htmlFile = AppDirectories.themeDir().resolve("default").resolve("index.html");

            LOGGER.info("Loading theme from: {}", htmlFile);

            // Check that the HTML file exists
            if (!Files.isRegularFile(htmlFile)) {
                throw new IllegalStateException("Theme HTML file not found: " + htmlFile);
            }

            // Monitor page loading
            webEngine.getLoadWorker().stateProperty().addListener(
                (observable, oldState, newState) -> {

                    LOGGER.info("WebView state: {}", newState);

                    if (newState == Worker.State.SUCCEEDED) {
                        LOGGER.info("Theme loaded successfully");
                    }

                    if (newState == Worker.State.FAILED) {
                        LOGGER.error(
                            "Failed to load theme",
                            webEngine.getLoadWorker().getException()
                        );
                    }
                }
            );

            // Monitor loading exceptions
            webEngine.getLoadWorker().exceptionProperty().addListener(
                (observable, oldException, newException) -> {

                    if (newException != null) {
                        LOGGER.error("WebView loading exception", newException);
                    }
                }
            );

            // Convert filesystem path into a proper file URL
            webEngine.load(htmlFile.toUri().toString());

            Scene scene = new Scene(webView, 800, 600);

            stage.setScene(scene);
            stage.setTitle("LibrePlayer");
            stage.show();

            LOGGER.info("Started UI");

        } catch (Exception e) {

            LOGGER.error("Error initializing UI", e);

        }
    }
}
