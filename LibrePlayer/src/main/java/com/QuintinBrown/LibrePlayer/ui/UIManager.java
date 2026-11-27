package com.QuintinBrown.LibrePlayer.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.QuintinBrown.LibrePlayer.library.AlbumMetadata;
import com.QuintinBrown.LibrePlayer.library.TrackMetadata;
import com.QuintinBrown.LibrePlayer.ui.theme.ThemeAPI;
import com.QuintinBrown.LibrePlayer.ui.theme.ThemeManager;
import com.google.gson.Gson;

import javafx.application.Platform;
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
    private final Gson gson = new Gson();
        

    private final WebView webView;
    private final WebEngine webEngine;

    private final LibraryController libraryController;
    private final LibraryAPI libraryAPI;

    private Logger logger = LoggerFactory.getLogger(UIManager.class);
    
    public UIManager(Stage stage, Controller controller) {

        this.webView = new WebView();
        this.webEngine = webView.getEngine();

        this.controller = controller;

        this.themeManager = new ThemeManager(webEngine);
        this.themeAPI = new ThemeAPI(themeManager);

        this.libraryController = new LibraryController(stage, controller.getDatabase());
        this.playerAPI = new PlayerAPI(controller, libraryController);
        this.libraryAPI = new LibraryAPI(libraryController, controller);

        webEngine.setOnAlert(event ->
            logger.info("[JavaScript Alert] " + event.getData())
        );

        setupJavaScriptBridge();

        setupPlaybackListener();

        setupUIEventListener();

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

                webEngine.executeScript(
                    "console.log('Java bridge registered');" +
                    "if (typeof window.onJavaBridgeReady === 'function') {" +
                    "window.onJavaBridgeReady();" +
                    "} else {" +
                    "console.error('onJavaBridgeReady callback not found');" +
                    "}"
                );

                initializeNowPlayingMetadata();

                Object uiType = webEngine.executeScript(
                    "typeof window.librePlayerUI"
                );

                Object callbackType = webEngine.executeScript(
                    "typeof window.onJavaBridgeReady"
                );

                Object titleElement = webEngine.executeScript(
                    "typeof document.getElementById('track-title')"
                );

                logger.info("librePlayerUI type: " + uiType);
                logger.info("onJavaBridgeReady type: " + callbackType);
                logger.info("track-title element type: " + titleElement);

                logger.info("getAlbums method type: " + result3);

                logger.info("importMusic method type: " + result2);

                logger.info("Player API type: " + result);
            }
        });
    }

    public void updateNowPlaying(
        String title,
        String artist,
        String album,
        String releaseDate,
        String artworkPath) {

        Platform.runLater(() -> {

            try {

                Object available = webEngine.executeScript(
                    "typeof window.librePlayerUI !== 'undefined' && " +
                    "typeof window.librePlayerUI.setTrack === 'function' && " +
                    "typeof window.librePlayerUI.setArtwork === 'function'"
                );

                if (!Boolean.TRUE.equals(available)) {
                    logger.debug(
                        "Now Playing interface unavailable; skipping update"
                    );
                    return;
                }

                // Update track metadata
                webEngine.executeScript(
                    "window.librePlayerUI.setTrack(" +
                    gson.toJson(title) + "," +
                    gson.toJson(artist) + "," +
                    gson.toJson(album) + "," +
                    gson.toJson(releaseDate) +
                    ")"
                );

                // Update album artwork
                webEngine.executeScript(
                    "window.librePlayerUI.setArtwork(" +
                    gson.toJson(artworkPath) +
                    ")"
                );

                logger.debug("Now Playing interface updated");

            } catch (Exception e) {

                logger.error(
                    "Failed to update Now Playing interface",
                    e
                );

            }

        });
    }

    private void initializeNowPlayingMetadata() {
        Platform.runLater(() -> {
            try {
                long trackID = controller.getCurrentTrackID();
                if (trackID <= 0) {
                    logger.debug("No current track to display");
                    return;
                }

                TrackMetadata track = controller.getTrackMetadata(trackID);

                if (track == null) {
                    logger.warn("No metadata for current track: {}", trackID);
                    return;
                }

                AlbumMetadata album = controller.getAlbumForTrack(trackID);

                String title = track.getTitle();
                String artist = String.join(", ", track.getArtists());

                String albumName = album != null ? album.getName() : null;
                String releaseDate = album != null ? album.getReleaseDate() : null;

                String coverPath = album != null && album.getCoverPath() != null ? album.getCoverPath().toString() : null;

                updateNowPlaying(title, artist, albumName, releaseDate, coverPath);
            } catch (Exception e) {
                logger.error("Failed to initialize Now Playing metadata", e);
            }
        });
    }

    public void updatePlaybackState(boolean playing, boolean paused) {
        Platform.runLater(() -> {
            try {
                Object available = webEngine.executeScript(
                    "typeof window.librePlayerUI !== 'undefined' && " +
                    "typeof window.librePlayerUI.setPlaybackState === 'function'"
                );

                if (!Boolean.TRUE.equals(available)) {
                    logger.debug("Playback interface unavailable; skipping update");
                    return;
                }

                webEngine.executeScript(
                    "window.librePlayerUI.setPlaybackState(" +
                    playing + "," + paused + ")"
                );

            } catch (Exception e) {
                logger.error("Failed to update playback state", e);
            }
        });
    }

    private void setupPlaybackListener() {

        controller.setPlaybackListener(new PlaybackListener() {

            @Override
            public void onTrackChanged(long trackID) {

                logger.info("Track changed: {}", trackID);

                TrackMetadata track = controller.getTrackMetadata(trackID);

                if (track == null) {
                    logger.warn("No metadata found for track ID: {}", trackID);
                    return;
                }

                AlbumMetadata album = controller.getAlbumForTrack(trackID);

                String title = track.getTitle();
                String artist = String.join(", ", track.getArtists());

                String albumName = album != null ? album.getName() : null;
                String releaseDate = album != null ? album.getReleaseDate() : null;

                String artworkPath = album != null && album.getCoverPath() != null
                        ? album.getCoverPath().toString()
                        : null;

                updateNowPlaying(
                        title,
                        artist,
                        albumName,
                        releaseDate,
                        artworkPath
                );
            }

            @Override
            public void onPlaybackStateChanged(
                    boolean playing,
                    boolean paused
            ) {

                updatePlaybackState(playing, paused);

            }

        });

    }

    private void setupUIEventListener() {
        controller.addUIEventListener(event -> {
            Platform.runLater(() -> {
                try {
                    Object available = webEngine.executeScript(
                        "typeof window.librePlayerUI !== 'undefined' && " +
                        "typeof window.librePlayerUI.handleBackendEvent == 'function'"
                    );

                    if (!Boolean.TRUE.equals(available)) {
                        return;
                    }

                    String eventJson = gson.toJson(event);

                    webEngine.executeScript(
                        "window.librePlayerUI.handleBackendEvent(" +
                        eventJson + ")"
                    );
                } catch (Exception e) {
                    logger.error("Failed to forward UI event: {}", event.getType(), e);
                }
            });
        });
    }

    public WebView getWebView(){return webView;}

    public Controller getController(){return controller;}

    public ThemeManager getThemeManager(){return themeManager;}

    public WebEngine getWebEngine(){return webEngine;}
}