package com.QuintinBrown.LibrePlayer.library;

import java.nio.file.Path;
import java.util.List;

public class TrackMetadata {
    private final String title;
    private final Path filePath;
    private final int lengthSeconds;
    private final int trackNumber;
    private final int discNumber;
    private final int channels;
    private final int sampleRate;
    private final int bitrate;
    private final String codec;
    private final List<String> artists;
    private final List<String> genres;

    public TrackMetadata(
            String title,
            Path filePath,
            int lengthSeconds,
            int trackNumber,
            int discNumber,
            int channels,
            int sampleRate,
            int bitrate,
            String codec,
            List<String> artists,
            List<String> genres) {

        this.title = title;
        this.filePath = filePath;
        this.lengthSeconds = lengthSeconds;
        this.trackNumber = trackNumber;
        this.discNumber = discNumber;
        this.channels = channels;
        this.sampleRate = sampleRate;
        this.bitrate = bitrate;
        this.codec = codec;
        this.artists = List.copyOf(artists);
        this.genres = genres;
    }

    public String getTitle(){return title;}
    public Path getFilePath(){return filePath;}
    public int getLengthSeconds(){return lengthSeconds;}
    public int getTrackNumber(){return trackNumber;}
    public int getDiscNumber(){return discNumber;}
    public int getChannels(){return channels;}
    public int getSampleRate(){return sampleRate;}
    public int getBitrate(){return bitrate;}
    public String getCodec(){return codec;}
    public List<String> getArtists(){return artists;}
    public List<String> getGenres(){return genres;}
}
