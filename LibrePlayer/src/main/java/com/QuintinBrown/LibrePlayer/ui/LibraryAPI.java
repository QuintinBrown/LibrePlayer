package com.QuintinBrown.LibrePlayer.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibraryAPI {
    private final LibraryController libraryController;

    private final Logger logger = LoggerFactory.getLogger(LibraryAPI.class);

    public LibraryAPI(LibraryController libraryController) {
        this.libraryController = libraryController;
        logger.info("LibraryAPI initialized");
    }

    public void importMusic() {
        logger.info("LibraryAPI: chooseMusicFolder called");
        libraryController.chooseMusicFolder();
    }

    public String getAlbums() {
        logger.info("Library API: getAlbums called");
        return libraryController.getAlbums();
    }
}
