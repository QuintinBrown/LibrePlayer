package com.QuintinBrown.LibrePlayer.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibraryAPI {
    private final LibraryController libraryController;

    private final Logger logger = LoggerFactory.getLogger(LibraryAPI.class);
    private final Controller controller;

    public LibraryAPI(LibraryController libraryController, Controller controller) {
        this.libraryController = libraryController;
        logger.info("LibraryAPI initialized");
        this.controller = controller;
    }

    public void importMusic() {
        logger.info("LibraryAPI: chooseMusicFolder called");
        libraryController.chooseMusicFolder();
    }

    public String getAlbums() {
        logger.info("Library API: getAlbums called");

        try {
            String result = libraryController.getAllAlbums();
            logger.info("Library API: getAlbums returned successfully");
            return result;
        } catch (Exception e) {
            logger.error("Library API: getAlbums failed", e);
            throw e;
        }
    }

    public String getAlbumTracks(String albumID) {
        logger.info("Library API: getAlbumTracks called");
        return libraryController.getAlbumTracks(Long.parseLong(albumID));
    }

    public void playAlbum(String albumID) {
        logger.info("LibraryAPI: playAlbum called with ID {}", albumID);
        controller.playAlbum(Long.parseLong(albumID));
    }
}
