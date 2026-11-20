package com.QuintinBrown.LibrePlayer.playback;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlaybackQueue {
    private long nextTrackID;
    private long currentTrackID;
    private long previousTrackID;
    private int currentIndex;

    private final List<Long> queue;

    private static final Logger LOGGER = LoggerFactory.getLogger(PlaybackQueue.class);


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
        if (index < 0 || index >= queue.size()) {
            return;
        }

        queue.remove(index);

        if (index < currentIndex) {
            currentIndex--;
        } else if (index == currentIndex) {
            currentIndex = -1;
            currentTrackID = -1;
        }
    }

    public void moveTrack(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= queue.size()
                || toIndex < 0 || toIndex >= queue.size()) {
            return;
        }

        Long trackID = queue.remove(fromIndex);
        queue.add(toIndex, trackID);

        if (currentIndex == fromIndex) {
            currentIndex = toIndex;
        } else if (fromIndex < currentIndex && toIndex >= currentIndex) {
            currentIndex--;
        } else if (fromIndex > currentIndex && toIndex <= currentIndex) {
            currentIndex++;
        }

        updateCurrentTrack();
    }

    public void clearQueue() {
        queue.clear();

        currentIndex = -1;
        currentTrackID = -1;
    }

    public void setQueue(List<Long> trackIDs) {
        queue.clear();
        queue.addAll(trackIDs);

        currentIndex = -1;
        currentTrackID = -1;

        LOGGER.info("Queue populated: {}", queue);
    }

    public Long next() {
        LOGGER.info("Queue size {}", queue.size());
        LOGGER.info("Current index before advancing: {}", currentIndex);
        if (queue.isEmpty()) {
            return null;
        }

        if (currentIndex + 1 >= queue.size()) {
            return null;
        }

        currentIndex++;

        updateCurrentTrack();

        LOGGER.info("Current index after advancing: {}", currentIndex);
        LOGGER.info("Current track ID: {}", currentTrackID);

        return currentTrackID;
    }

    public Long previous() {
        if (queue.isEmpty()) {
            return null;
        }

        if (currentIndex <= 0) {
            return null;
        }

        currentIndex--;

        updateCurrentTrack();

        return currentTrackID;
    }

    public void setCurrentTrack(long trackID) {

        int index = queue.indexOf(trackID);

        if (index == -1) {
            return;
        }

        currentIndex = index;
        updateCurrentTrack();
    }

    private void updateCurrentTrack() {

        if (currentIndex >= 0 && currentIndex < queue.size()) {
            currentTrackID = queue.get(currentIndex);
        } else {
            currentTrackID = -1;
        }
    }

    public long getNextTrack(){return nextTrackID;}
    public long getCurrentTrack(){return currentTrackID;}
    public long getCurrentIndex(){return currentIndex;}
    public List<Long> getQueue() {return new ArrayList<>(queue);}
    public int getQueueSize(){return queue.size();}
    public long getPreviousTrack(){return previousTrackID;}
}
