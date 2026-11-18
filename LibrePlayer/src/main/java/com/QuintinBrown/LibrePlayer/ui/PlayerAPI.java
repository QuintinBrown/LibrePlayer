package com.QuintinBrown.LibrePlayer.ui;

import com.QuintinBrown.LibrePlayer.playback.PlaybackState;
import com.QuintinBrown.LibrePlayer.playback.Player;

public class PlayerAPI {

    private final Controller controller;
    private final Player player;

    public PlayerAPI(Controller controller) {
        this.controller = controller;
        this.player = controller.getPlayer();
    }

    // Playback controls

    public void openFile() {
        System.out.println("PlayerAPI: openFile called");
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