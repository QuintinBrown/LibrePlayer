package com.QuintinBrown.LibrePlayer.ui;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.QuintinBrown.LibrePlayer.library.AlbumMetadata;
import com.QuintinBrown.LibrePlayer.library.DatabaseManager;
import com.QuintinBrown.LibrePlayer.library.ImportManager;
import com.QuintinBrown.LibrePlayer.playback.PlaybackQueue;
import com.QuintinBrown.LibrePlayer.playback.PlaybackState;
import com.QuintinBrown.LibrePlayer.playback.Player;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

public class Controller {

    private final Player PLAYER;
    private final ImportManager importManager;
    private final DatabaseManager database = new DatabaseManager();
    private final PlaybackQueue playbackQueue;
    private final Logger logger = LoggerFactory.getLogger(Controller.class);


    public Controller() {
        this.playbackQueue = new PlaybackQueue();
        this.PLAYER = new Player(database);
        this.importManager = new ImportManager(database);

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
    }

    public void pause() {
        PLAYER.pause();
    }

    public void playNextTrack() {

        Long trackID = playbackQueue.next();

        if (trackID == null) {
            PLAYER.stop();
            return;
        }
        
        PLAYER.open(trackID);
        PLAYER.play();
    }

    public void playPreviousTrack() {

        Long trackID = playbackQueue.previous();

        if (trackID == null) {
            return;
        }

        PLAYER.open(trackID);
        PLAYER.play();
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

        playNextTrack();
    }

    public void togglePlayPause() {
        if (PLAYER.getState() == PlaybackState.PLAYING) {
            PLAYER.pause();
        } else {
            PLAYER.play();
        }
    }

    public String importMusic() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Music Folder");

        File folder = chooser.showDialog(null);

        if (folder != null) {
            importManager.importLibrary(folder.toPath());
        } else {
            return null;
        }

        return folder.getAbsolutePath();
    }

    public void shutdown() {
        PLAYER.close();
    }

    public Player getPlayer() {
        return PLAYER;
    }
}