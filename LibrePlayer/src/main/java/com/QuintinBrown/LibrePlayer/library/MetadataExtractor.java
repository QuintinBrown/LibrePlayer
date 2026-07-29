package com.QuintinBrown.LibrePlayer.library;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetadataExtractor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataExtractor.class);

     
    public AlbumMetadata extractAlbum(AlbumFolder folder) {

        List<TrackMetadata> tracks = new ArrayList<>();

        String albumName = null;
        String releaseDate = "";
        List<String> artists = null;
        List<String> genres = null;

        int totalLength = 0;
        int highestDisc = 1;

        for (Path file : folder.getTracks()) {

            TrackMetadata track = extractTrack(file);
            tracks.add(track);

            totalLength += track.getLengthSeconds();
            highestDisc = Math.max(highestDisc, track.getDiscNumber());

            try {
                AudioFile audioFile = AudioFileIO.read(file.toFile());
                Tag tag = audioFile.getTag();

                if (albumName == null) {
                    albumName = tag.getFirst(FieldKey.ALBUM);
                }

                if (artists == null) {
                    artists = tag.getAll(FieldKey.ALBUM_ARTISTS);
                }

                if (genres == null) {
                    genres = tag.getAll(FieldKey.GENRE);
                }

                if (releaseDate.isBlank()) {
                    releaseDate = tag.getFirst(FieldKey.YEAR);
                }

            } catch (Exception e) {
                LOGGER.warn("Failed reading album metadata from {}", file, e);
            }
        }

        if (albumName == null || albumName.isBlank()) {
            albumName = folder.getFolder().getFileName().toString();
        }

        return new AlbumMetadata(
                albumName,
                folder.getFolder(),
                folder.getCoverPath(),
                releaseDate,
                totalLength,
                highestDisc,
                tracks,
                artists,
                genres
        );
    }

    private TrackMetadata extractTrack(Path file) {

        try {

            AudioFile audioFile = AudioFileIO.read(file.toFile());

            Tag tag = audioFile.getTag();
            AudioHeader header = audioFile.getAudioHeader();

            return new TrackMetadata(

                    emptyToFilename(tag.getFirst(FieldKey.TITLE), file),

                    file,

                    header.getTrackLength(),

                    parseInt(tag.getFirst(FieldKey.TRACK)),

                    parseInt(tag.getFirst(FieldKey.DISC_NO)),

                    header.getChannels().equals("stereo") ? 2 : 1,

                    header.getSampleRateAsNumber(),

                    parseBitrate(header.getBitRate()),

                    header.getFormat(),

                    splitValues(tag.getFirst(FieldKey.ARTIST)),

                    splitValues(tag.getFirst(FieldKey.GENRE))

            );

        } catch (Exception e) {

            LOGGER.error("Failed reading metadata from " + file + " " + e);
            throw new RuntimeException(e);
        }
    }

    private List<String> splitValues(String value) {

        if (value == null || value.isBlank()) {
            return List.of();
        }

        return List.of(value.split("\\s*;\\s*"));
    }

    private int parseInt(String value) {

        if (value == null || value.isBlank()) {
            return 0;
        }

        try {

            if (value.contains("/")) {
                value = value.substring(0, value.indexOf('/'));
            }

            return Integer.parseInt(value.trim());

        } catch (NumberFormatException e) {

            return 0;
        }
    }

    private int parseBitrate(String bitrate) {

        if (bitrate == null) {
            return 0;
        }

        bitrate = bitrate.replaceAll("[^0-9]", "");

        if (bitrate.isBlank()) {
            return 0;
        }

        return Integer.parseInt(bitrate);
    }

    private String emptyToFilename(String title, Path file) {

        if (title == null || title.isBlank()) {

            String filename = file.getFileName().toString();

            int dot = filename.lastIndexOf('.');

            if (dot > 0) {
                return filename.substring(0, dot);
            }

            return filename;
        }

        return title;
    }
}

