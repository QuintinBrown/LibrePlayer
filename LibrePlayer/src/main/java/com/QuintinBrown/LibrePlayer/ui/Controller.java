package com.QuintinBrown.LibrePlayer.ui;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.QuintinBrown.LibrePlayer.library.AlbumMetadata;
import com.QuintinBrown.LibrePlayer.library.DatabaseManager;
import com.QuintinBrown.LibrePlayer.library.TrackMetadata;
import com.QuintinBrown.LibrePlayer.playback.PlaybackQueue;
import com.QuintinBrown.LibrePlayer.playback.PlaybackState;
import com.QuintinBrown.LibrePlayer.playback.Player;
import com.QuintinBrown.LibrePlayer.ui.event.UIEvent;
import com.QuintinBrown.LibrePlayer.ui.event.UIEventListener;
import com.QuintinBrown.LibrePlayer.ui.event.UIEventType;

import javafx.stage.FileChooser;

public class Controller {

    private final Player PLAYER;
    private final DatabaseManager database = new DatabaseManager();
    private final PlaybackQueue playbackQueue;
    private final Logger logger = LoggerFactory.getLogger(Controller.class);
    private PlaybackListener playbackListener;
    private Long currentTrackID;

    private final CopyOnWriteArrayList<UIEventListener> uiEventListeners = new CopyOnWriteArrayList<>();


    public Controller() {
        this.playbackQueue = new PlaybackQueue();
        this.PLAYER = new Player(database);

        PLAYER.setOnTrackFinished(this::playNextTrack);
    }

    public void openFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Audio File");

        File file = chooser.showOpenDialog(null);

        if (file != null) {
            PLAYER.open(file.toPath());
        }
    }

    public void play() {
        PLAYER.play();
        notifyPlaybackState();
    }

    public void pause() {
        PLAYER.pause();
        notifyPlaybackState();
    }

    public void playTrack(long trackID) {
        currentTrackID = trackID;
        PLAYER.open(trackID);
        PLAYER.play();

        notifyTrackChanged(trackID);
        notifyPlaybackState();
    }

    public void playNextTrack() {

        Long trackID = playbackQueue.next();

        if (trackID == null) {
            currentTrackID = null;
            PLAYER.stop();
            notifyUI(UIEventType.TRACK_CHANGED, null);
            notifyQueueChanged();
            notifyPlaybackState();
            return;
        }

        currentTrackID = trackID;
        
        PLAYER.open(trackID);
        PLAYER.play();

        notifyTrackChanged(trackID);
        notifyQueueIndexChanged();
        notifyPlaybackState();
    }

    public void playPreviousTrack() {

        Long trackID = playbackQueue.previous();

        if (trackID == null) {
            return;
        }

        currentTrackID = trackID;

        PLAYER.open(trackID);
        PLAYER.play();

        notifyTrackChanged(trackID);
        notifyQueueIndexChanged();
        notifyPlaybackState();
    }

    public void playAlbum(long albumID) {
        logger.info("Controller: playAlbum called with album ID {}", albumID);
        AlbumMetadata album = database.getAlbum(albumID);

        if (album == null || album.getTracks().isEmpty()) {
            logger.info("Album not found, id: ", albumID);
            return;
        }

        logger.info("Album with name {} found", album.getName());
        logger.info("Track count: {}", album.getTracks().size());
        logger.info("Track IDs: {}", album.getTracks());

        playbackQueue.setQueue(album.getTracks());

        notifyQueueChanged();

        playNextTrack();
    }

    public void togglePlayPause() {
        if (PLAYER.getState() == PlaybackState.PLAYING) {
            PLAYER.pause();
        } else {
            PLAYER.play();
        }

        notifyPlaybackState();
    }

    private void notifyTrackChanged(long trackID) {

        notifyUI(UIEventType.TRACK_CHANGED, trackID);

        if (playbackListener != null) {
            playbackListener.onTrackChanged(trackID);
        }

    }

    private void notifyPlaybackState() {

        PlaybackState state = PLAYER.getState();

        notifyUI(UIEventType.PLAYBACK_STATE_CHANGED, state);

        if (playbackListener != null) {
            playbackListener.onPlaybackStateChanged(
                state == PlaybackState.PLAYING,
                state == PlaybackState.PAUSED
            );

        }

    }

    public AlbumMetadata getAlbumForTrack(long trackID) {
        return database.getAlbumFromTrack(trackID);
    }

    public long getQueueIndex() {
        return playbackQueue.getCurrentIndex();
    }

    public void shutdown() {
        PLAYER.close();
    } 

    public void setPlaybackListener(PlaybackListener playbackListener) {
        this.playbackListener = playbackListener;
    }

    public TrackMetadata getTrackMetadata(long trackID) {
        return database.getTrack(trackID);
    }

    public void emptyQueue() {
        playbackQueue.clearQueue();
        currentTrackID = null;

        notifyQueueChanged();
        notifyPlaybackState();
    }

    public Player getPlayer() {
        return PLAYER;
    }

    public void playTrackAtQueue(int index) {
        playbackQueue.skipToIndex(index);

        currentTrackID = playbackQueue.getCurrentTrack();
        
        PLAYER.open(playbackQueue.getCurrentTrack());
        PLAYER.play();

        notifyTrackChanged(playbackQueue.getCurrentTrack());
        notifyQueueIndexChanged();
        notifyPlaybackState();
    }

    public void addUIEventListener(UIEventListener listener) {
        uiEventListeners.add(listener);
    }

    public void removeUIEventListener(UIEventListener listener) {
        uiEventListeners.remove(listener);
    }

    private void notifyUI(UIEventType type, Object data) {
        UIEvent event = new UIEvent(type, data);

        for (UIEventListener listener : uiEventListeners) {
            try {
                listener.onUIEvent(event);
            } catch (Exception e) {
                logger.error("Failed to notify UI listener for: {}", type, e);
            }
        }
    }

    private void notifyQueueChanged() {
        notifyUI(UIEventType.QUEUE_CHANGED, getPlaybackQueue());

        notifyUI(UIEventType.QUEUE_INDEX_CHANGED, getQueueIndex());
    }

    private void notifyQueueIndexChanged() {
        notifyUI(UIEventType.QUEUE_INDEX_CHANGED, getQueueIndex());
    }

    public List<Long> getPlaybackQueue(){return playbackQueue.getQueue();}

    public Long getCurrentTrackID(){return currentTrackID;}

    public DatabaseManager getDatabase() {return database;}
}
