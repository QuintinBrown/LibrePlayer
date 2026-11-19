package com.QuintinBrown.LibrePlayer.library;

import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportManager {
    private final FileScanner scanner;
    private final MetadataExtractor extractor;
    private final DatabaseManager database;

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportManager.class);

    public ImportManager(DatabaseManager database) {
        this.scanner = new FileScanner();
        this.extractor = new MetadataExtractor();
        this.database = database;
    }

    private void importAlbum(AlbumFolder folder) {
        LOGGER.info("Importing album from " + folder.getFolder());

        AlbumExtractionMetadata album = extractor.extractAlbum(folder);

        database.beginTransaction();

        try {
            long albumID = database.insertAlbum(album);

            for (TrackMetadata track : album.getTracks()) {
                long trackID = database.insertTrack(albumID, track);
            }

            database.commit();
            LOGGER.info("Imported album " + album.getName());

        } catch (Exception e) {
            LOGGER.error("Rolling back album " + album.getName(), e);

            database.rollback();

            throw e;
        }
    }

    public void importLibrary(Path rootFolder) {
        LOGGER.info("Starting library import from " + rootFolder);

        List<AlbumFolder> folders = scanner.scan(rootFolder);

        LOGGER.info("Found " + folders.size() + " albums");

        for (AlbumFolder folder : folders) {
            try {
                importAlbum(folder);
            } catch (Exception e) {
                LOGGER.error("Failed importing album " + folder.getFolder() + " " + e);
            }
        }

        LOGGER.info("Library import finished");
    }
}
