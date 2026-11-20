package com.QuintinBrown.LibrePlayer.ui;

public interface PlaybackListener {
    void onTrackChanged(long trackID);
    void onPlaybackStateChanged(boolean playing, boolean paused);
}
