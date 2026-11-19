package com.QuintinBrown.LibrePlayer.library;

import java.nio.file.Path;
import java.util.List;

public class AlbumExtractionMetadata {
    private final String name;
    private final Path folderPath;
    private final Path coverPath;
    private final String releaseDate;
    private final int lengthSeconds;
    private final int discCount;
    private final List<String> albumArtists;
    private final List<String> genres;

    private final List<TrackMetadata> tracks;

    public AlbumExtractionMetadata(
            String name,
            Path folderPath,
            Path coverPath,
            String releaseDate,
            int lengthSeconds,
            int discCount,
            List<TrackMetadata> tracks,
            List<String> albumArtists,
            List<String> genres) {

        this.name = name;
        this.folderPath = folderPath;
        this.coverPath = coverPath;
        this.releaseDate = releaseDate;
        this.lengthSeconds = lengthSeconds;
        this.discCount = discCount;
        this.tracks = List.copyOf(tracks);
        this.albumArtists = albumArtists;
        this.genres = genres;
    }

    public String getName(){return name;}
    public Path getFolderPath(){return folderPath;}
    public Path getCoverPath(){return coverPath;}
    public String getReleaseDate(){return releaseDate;}
    public int getLengthSeconds(){return lengthSeconds;}
    public int getDiscCount(){return discCount;}
    public List<TrackMetadata> getTracks(){return tracks;}
    public List<String> getAlbumArtists(){return albumArtists;}
    public List<String> getGenres(){return genres;}
}
