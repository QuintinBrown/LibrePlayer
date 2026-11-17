package com.QuintinBrown.LibrePlayer.ui;

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

        webEngine.load(
            getClass().getResource("/ui/index.html").toExternalForm()
        );

        return webView;
    }
}