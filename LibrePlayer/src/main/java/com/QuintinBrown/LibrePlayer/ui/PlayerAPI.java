package com.QuintinBrown.LibrePlayer.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.QuintinBrown.LibrePlayer.playback.PlaybackState;
import com.QuintinBrown.LibrePlayer.playback.Player;

public class PlayerAPI {

    private final Controller controller;
    private final Player player;

    private final Logger logger = LoggerFactory.getLogger(PlayerAPI.class);

    public PlayerAPI(Controller controller) {
        this.controller = controller;
        this.player = controller.getPlayer();
    }

    // Playback controls

    public void openFile() {
        logger.info("PlayerAPI: openFile called");
        controller.openFile();
    }

    public void play() {
        controller.play();
    }

    public void pause() {
        controller.pause();
    }

    public void togglePlayPause() {
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

    // Library actions

    public String importMusic() {
        return controller.importMusic();
    }
}