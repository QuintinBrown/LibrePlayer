package com.QuintinBrown.LibrePlayer.ui;

public class LibraryAPI {
    private final LibraryController libraryController;

    public LibraryAPI(LibraryController libraryController) {
        this.libraryController = libraryController;
        System.out.println("LibraryAPI initialized");
    }

    public void importMusic() {
        System.out.println("LibraryAPI: chooseMusicFolder called");
        libraryController.chooseMusicFolder();
    }
}
