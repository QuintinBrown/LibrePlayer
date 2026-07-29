package com.QuintinBrown.LibrePlayer.library;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileScanner.class);

    private static final List<String> AUDIO_EXTENSIONS = List.of(
            ".mp3",
            ".flac",
            ".wav",
            ".ogg",
            ".opus",
            ".m4a",
            ".aac",
            ".alac"
    );

    private static final List<String> COVER_EXTENSIONS = List.of(
            ".jpg",
            ".jpeg",
            ".png",
            ".webp"
    );

    private boolean isAudioFile(Path file) {

        String name = file.getFileName().toString().toLowerCase();

        return AUDIO_EXTENSIONS.stream().anyMatch(name::endsWith);
    }


    private boolean isCoverImage(Path file) {

        String name = file.getFileName().toString().toLowerCase();

        return COVER_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    private boolean isPreferredCover(Path file) {

        String name = file.getFileName().toString().toLowerCase();

        return (name.equals("cover.jpg") || name.equals("cover.png"));
    }

    private AlbumFolder scanAlbumFolder(Path folder) {
        List<Path> tracks = new ArrayList<>();
        Path cover = null;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path file : stream) {
                if (!Files.isRegularFile(file)) {
                    continue;
                }

                if (isAudioFile(file)) {
                    tracks.add(file);
                } else if (isCoverImage(file)) {
                    if (cover == null || isPreferredCover(file)) {
                        cover = file;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed scanning folder " + e);
            return null;
        }

        if (tracks.isEmpty()) {
            LOGGER.info("Skipping " + folder + " as there are no audio files found");
            return null;
        }

        tracks.sort(Path::compareTo);

        return new AlbumFolder(folder, cover, tracks);
    }

    public List<AlbumFolder> scan(Path rootDirectory) {
        List<AlbumFolder> albums = new ArrayList<>();

        if (!Files.exists(rootDirectory)) {
            LOGGER.error("Music directory does not exist " + rootDirectory);
            return albums;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootDirectory)) {
            for (Path folder : stream) {
                if (Files.isDirectory(folder)) {
                    AlbumFolder album = scanAlbumFolder(folder);

                    if (album != null) {
                        albums.add(album);
                    }
                }
            }
            
        } catch (IOException e) {
            LOGGER.error("Failed to scan directory " + rootDirectory + " " + e);
        }

        return albums;
    }
}
