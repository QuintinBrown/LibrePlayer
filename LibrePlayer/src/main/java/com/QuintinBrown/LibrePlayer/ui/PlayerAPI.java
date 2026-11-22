package com.QuintinBrown.LibrePlayer.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.QuintinBrown.LibrePlayer.playback.PlaybackState;
import com.QuintinBrown.LibrePlayer.playback.Player;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PlayerAPI {

    private final Controller controller;
    private final Player player;
    private final LibraryController libraryController;

    private final Logger logger = LoggerFactory.getLogger(PlayerAPI.class);

    public PlayerAPI(Controller controller, LibraryController libraryController) {
        this.controller = controller;
        this.player = controller.getPlayer();
        this.libraryController = libraryController;
    }

    // Playback controls

    public void openFile() {
        logger.info("PlayerAPI: openFile called");
        controller.openFile();
    }

    public void play() {
        logger.info("PlayerAPI: play called");
        controller.play();
    }

    public void pause() {
        logger.info("PlayerAPI: pause called");
        controller.pause();
    }

    public void togglePlayPause() {
        logger.info("PlayerAPI: togglePlayPause called");
        controller.togglePlayPause();
    }
    
    // Playback information

    public boolean isPlaying() {
        return player.getState() == PlaybackState.PLAYING;
    }

    public boolean isPaused() {
        return player.getState() == PlaybackState.PAUSED;
    }

    public boolean isStopped() {
        return player.getState() == PlaybackState.STOPPED;
    }

    public void playTrack(String trackID) {

        logger.info("LibraryAPI: playTrack called with ID {}", trackID);
        controller.playTrack(Long.parseLong(trackID));
    }

    public Long getCurrentTrackID() {
        return controller.getCurrentTrackID();
    }

    public String getCurrentTrackMetadata() {

        Long trackID = controller.getCurrentTrackID();

        if (trackID == null) {
            logger.warn("No current track ID available");
            return null;
        }

        String metadata = libraryController.getTrackMetadata(trackID);
        logger.info("Current track metadata {}", metadata);

        return metadata;
    }

    public String getPlaybackQueue() {
        JsonArray queue = new JsonArray();

        for (Long trackID : controller.getPlaybackQueue()) {
            String metadata = libraryController.getTrackMetadata(trackID);

            if (metadata == null) {
                continue;
            }

            JsonObject track = JsonParser.parseString(metadata).getAsJsonObject();
            track.addProperty("id", trackID);
            queue.add(track);
        }

        return queue.toString();
    }
}