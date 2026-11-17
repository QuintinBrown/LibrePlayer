package com.QuintinBrown.LibrePlayer.ui;

import java.nio.file.Files;
import java.nio.file.Path;

import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public class UIManager {

    private final Controller controller;
    private final PlayerAPI playerAPI;

    private WebView webView;
    private WebEngine webEngine;

    public UIManager() {
        this.controller = new Controller();
        this.playerAPI = new PlayerAPI(controller);
    }

    public WebView createUI() {

        webView = new WebView();
        webEngine = webView.getEngine();

        webEngine.getLoadWorker().stateProperty().addListener(
            (obs, oldState, newState) -> {

                if (newState == Worker.State.SUCCEEDED) {

                    JSObject window = (JSObject)
                        webEngine.executeScript("window");

                    window.setMember("player", playerAPI);
                }
            }
        );

        return webView;
    }

    public void loadTheme(Path themeDirectory) {
        Path htmlFile = themeDirectory.resolve("index.html");

        if (!Files.exists(htmlFile)) throw new IllegalArgumentException("Theme does not contain index.html");
        webEngine.load(htmlFile.toUri().toString());
    }
}