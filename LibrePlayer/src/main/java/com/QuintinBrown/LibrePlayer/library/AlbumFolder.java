package com.QuintinBrown.LibrePlayer.library;

import java.nio.file.Path;
import java.util.List;

public class AlbumFolder {

    private final Path folder;
    private final Path coverPath;
    private final List<Path> tracks;

    public AlbumFolder(Path folder, Path coverPath, List<Path> tracks) {
        this.folder = folder;
        this.coverPath = coverPath;
        this.tracks = List.copyOf(tracks);
    }

    public Path getFolder() {return folder;}
    public Path getCoverPath() {return coverPath;}
    public List<Path> getTracks() {return tracks;}
}
