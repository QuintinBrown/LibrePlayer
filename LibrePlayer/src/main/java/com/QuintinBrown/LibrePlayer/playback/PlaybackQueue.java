package com.QuintinBrown.LibrePlayer.playback;

import java.util.ArrayList;
import java.util.List;

public class PlaybackQueue {
    private long nextTrackID;
    private long currentTrackID;
    private long previousTrackID;
    private int currentIndex;

    private final List<Long> queue;

    public PlaybackQueue() {
        this.queue = new ArrayList<>();
        this.currentIndex = -1;
    }

    public void addTrack(Long trackID) {
        queue.add(trackID);
    }

    public void addTracks(List<Long> trackIDs) {
        for (Long trackID : trackIDs) {
            queue.add(trackID);
        }
    }

    public void insertNext(Long trackID) {
        int trackPosition = currentIndex + 1;
        queue.add(trackPosition, trackID);
    }

    public void removeTrack(int index) {
        queue.remove(index);
    }

    public void moveTrack(int fromIndex, int toIndex) {
        long trackID = queue.get(fromIndex);
        queue.remove(fromIndex);

        queue.add(toIndex, trackID);
    }

    public void clearQueue() {
        if (!queue.isEmpty()) {
            queue.clear();
        }
    }

    public long getNextTrack(){return nextTrackID;}
    public long getCurrentTrack(){return currentTrackID;}
    public long getPreviousTrack(){return previousTrackID;}
}
