package com.QuintinBrown.LibrePlayer.ui;

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

    
    public UIManager(Stage stage) {

        this.webView = new WebView();
        this.webEngine = webView.getEngine();

        this.controller = new Controller();
        this.playerAPI = new PlayerAPI(controller);

        this.themeManager = new ThemeManager(webEngine);
        this.themeAPI = new ThemeAPI(themeManager);

        this.libraryController = new LibraryController(stage);
        this.libraryAPI = new LibraryAPI(libraryController);

        webEngine.setOnAlert(event ->
            System.out.println("[JavaScript Alert] " + event.getData())
        );

        setupJavaScriptBridge();

        System.out.println("UIManager initialized");
    }


   private void setupJavaScriptBridge() {

    webEngine.getLoadWorker().stateProperty().addListener(
        (observable, oldState, newState) -> {

            if (newState == Worker.State.SUCCEEDED) {

                System.out.println("Registering JavaScript bridge");

                JSObject window = (JSObject)
                    webEngine.executeScript("window");

                window.setMember("player", playerAPI);
                window.setMember("themes", themeAPI);
                window.setMember("library", libraryAPI);

                System.out.println("Player API registered");
                System.out.println("Library API registered");

                Object result = webEngine.executeScript(
                    "typeof window.player"
                );

                Object result2 = webEngine.executeScript(
                    "typeof window.library.importMusic"
                );

                System.out.println("importMusic method type: " + result2);

                System.out.println("Player API type: " + result);
            }
        });
    }

    public WebView getWebView(){return webView;}

    public Controller getController(){return controller;}

    public ThemeManager getThemeManager(){return themeManager;}

    public WebEngine getWebEngine(){return webEngine;}
}