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
        return libraryController.getAlbums();
    }

    public void playAlbum(long albumID) {
        logger.info("LibraryAPI: playAlbum called with ID {}", albumID);
        controller.playAlbum(albumID);
    }
}
