package com.QuintinBrown.LibrePlayer.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.QuintinBrown.LibrePlayer.ui.theme.ThemeAPI;
import com.QuintinBrown.LibrePlayer.ui.theme.ThemeManager;

import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

public class UIManager {

    private final Controller controller;
    private final PlayerAPI playerAPI;
    private final ThemeManager themeManager;
    private final ThemeAPI themeAPI;
        

    private final WebView webView;
    private final WebEngine webEngine;

    private final LibraryController libraryController;
    private final LibraryAPI libraryAPI;

    private Logger logger = LoggerFactory.getLogger(UIManager.class);
    
    public UIManager(Stage stage, Controller controller) {

        this.webView = new WebView();
        this.webEngine = webView.getEngine();

        this.controller = controller;
        this.playerAPI = new PlayerAPI(controller);

        this.themeManager = new ThemeManager(webEngine);
        this.themeAPI = new ThemeAPI(themeManager);

        this.libraryController = new LibraryController(stage);
        this.libraryAPI = new LibraryAPI(libraryController);

        webEngine.setOnAlert(event ->
            logger.info("[JavaScript Alert] " + event.getData())
        );

        setupJavaScriptBridge();

        logger.info("UIManager initialized");
    }


   private void setupJavaScriptBridge() {

    webEngine.getLoadWorker().stateProperty().addListener(
        (observable, oldState, newState) -> {

            if (newState == Worker.State.SUCCEEDED) {

                logger.info("Registering JavaScript bridge");

                JSObject window = (JSObject)
                    webEngine.executeScript("window");

                window.setMember("player", playerAPI);
                window.setMember("themes", themeAPI);
                window.setMember("library", libraryAPI);

                window.setMember("javaConsole", new Object() {

                    public void log(String message) {
                        System.out.println("[JS] " + message);
                    }

                    public void error(String message) {
                        System.err.println("[JS ERROR] " + message);
                    }

                });

                logger.info("Player API registered");
                logger.info("Library API registered");

                Object result = webEngine.executeScript(
                    "typeof window.player"
                );

                Object result2 = webEngine.executeScript(
                    "typeof window.library.importMusic"
                );

                Object result3 = webEngine.executeScript(
                    "typeof window.library.getAlbums"
                );

                logger.info("getAlbums method type: " + result3);

                logger.info("importMusic method type: " + result2);

                logger.info("Player API type: " + result);
            }
        });
    }

    public WebView getWebView(){return webView;}

    public Controller getController(){return controller;}

    public ThemeManager getThemeManager(){return themeManager;}

    public WebEngine getWebEngine(){return webEngine;}
}