
package com.QuintinBrown.LibrePlayer.ui;

import org.bytedeco.ffmpeg.global.avutil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(App.class);

    private UIManager uiManager;
    private Controller controller;

    @Override
    public void start(Stage stage) {

        try {
            LOGGER.info("FFmpeg version: {}",
                avutil.avutil_version());

            LOGGER.info("FFmpeg native initialization successful");

        } catch (Throwable t) {
            LOGGER.error("FFmpeg native initialization failed", t);
        }

        try {
            controller = new Controller();

            uiManager = new UIManager(stage, controller);

            Scene scene = new Scene(
                uiManager.getWebView(),
                1000,
                700
            );

            stage.setScene(scene);
            stage.setTitle("LibrePlayer");
            stage.show();

            // Load the default theme using UIManager's ThemeManager.
            uiManager.getThemeManager().setTheme("default");

            LOGGER.info("Started UI");

        } catch (Exception e) {
            LOGGER.error("Error initializing UI", e);
        }
    }

    @Override
    public void stop() {
        LOGGER.info("Application shutting down");

        if (controller != null) {
            controller.shutdown();
        }
    }
}
