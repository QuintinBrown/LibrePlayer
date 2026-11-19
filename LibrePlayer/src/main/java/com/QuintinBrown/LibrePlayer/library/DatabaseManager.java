package com.QuintinBrown.LibrePlayer.library;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseManager.class);
    private static final Path DB_PATH = AppDirectories.databaseDir().resolve("library.db");
    private static final String URL = "jdbc:sqlite:" + DB_PATH.toAbsolutePath();

    private static Connection conn;

    public static void openConnection() {
        LOGGER.info("Attempting to start connection");
        if (conn != null) {
            return;
        }

        try {
            conn = DriverManager.getConnection(URL);

            conn.createStatement().execute("PRAGMA foreign_keys = ON");
            conn.createStatement().execute("PRAGMA busy_timeout = 5000");

            if (conn.getAutoCommit()) {
                conn.setAutoCommit(false);
            }

            LOGGER.info("Connected to database " + DB_PATH);

        } catch (SQLException e) {
            LOGGER.error("Failed to open database connection " + e);
            throw new RuntimeException(e);
        }
    }

    public void beginTransaction() {
        if (conn == null) {
            openConnection();
        }

        try {
            if (conn.getAutoCommit()) {
                conn.setAutoCommit(false);
                LOGGER.info("Transaction started.");
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to begin transaction" + e);
            throw new RuntimeException(e);
        }
    }

    public void commit() {

        try {
            conn.commit();
            conn.setAutoCommit(false);
            LOGGER.debug("Transaction committed.");
        } catch (SQLException e) {
            LOGGER.error("Failed to commit transaction.", e);
        }
    }

    public void rollback() {

        try {
            conn.rollback();
            LOGGER.debug("Transaction rolled back");
        } catch (SQLException e) {
            LOGGER.error("Failed to rollback transaction" + e);
            throw new RuntimeException(e);
        }
    }

    public static void initializeDB() {
        LOGGER.info("Attempting to initialize database");
        if (conn == null) {
            openConnection();
        }

        try (InputStream in = DatabaseManager.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("Could not find schema.sql");
            }

            String schema = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            String[] statements = schema.split(";");

            try (Statement stmt = conn.createStatement()) {
                for (String statement : statements) {
                    statement = statement.trim();

                    if (!statement.isEmpty()) {
                        stmt.executeUpdate(statement);
                    }
                }

                conn.commit();
                
            } catch (SQLException e) {
                LOGGER.error("Failed to initialize DB " + e);
                throw new RuntimeException("Database schema initialization failed", e);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read schema file " + e);
        }

        LOGGER.info("Database created");
    }

    public void close() {
        if (conn == null) {
            return;
        }

        try {
            conn.close();
            LOGGER.info("Database connection closed");
        } catch (SQLException e) {
            LOGGER.error("Failed to close connection " + e);
        } finally {
            conn = null;
        }
    }

    public long insertTrack(long albumId, TrackMetadata track) {

        if (conn == null) {
            openConnection();
        }

        String sql = """
            INSERT INTO song (
                title,
                length,
                filepath,
                playcount,
                last_listen,
                first_added,
                last_modified,
                file_size,
                disc_number,
                channels,
                codec,
                sample_rate,
                bitrate,
                isFavorite,
                album_id,
                track_number
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, track.getTitle());
            stmt.setInt(2, track.getLengthSeconds());
            stmt.setString(3, track.getFilePath().toString());

            stmt.setInt(4, 0);

            stmt.setNull(5, Types.INTEGER);

            stmt.setLong(6, Instant.now().getEpochSecond());

            stmt.setLong(7, Files.getLastModifiedTime(track.getFilePath()).toInstant().getEpochSecond());

            stmt.setLong(8, Files.size(track.getFilePath()));

            stmt.setInt(9, track.getDiscNumber());

            stmt.setInt(10, track.getChannels());

            stmt.setString(11, track.getCodec());

            stmt.setInt(12, track.getSampleRate());

            stmt.setInt(13, track.getBitrate());

            stmt.setInt(14, 0);

            stmt.setLong(15, albumId);

            stmt.setInt(16, track.getTrackNumber());

            stmt.executeUpdate();

            long track_id = -1;

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    track_id = keys.getLong(1);
                }
            }

            if (track_id == -1) {
                throw new SQLException("No track ID returned");
            }

            String artist_sql = """
            INSERT INTO artist (
                name
            )
            VALUES (?)
            ON CONFLICT(name) DO NOTHING
                """;

            String artist_select = """
                    SELECT id
                    from artist
                    WHERE name = ?
                    """;

            List<Long> artist_ids = new ArrayList<>();

            try (PreparedStatement artist_stmt = conn.prepareStatement(artist_sql);
                 PreparedStatement artist_select_stmt = conn.prepareStatement(artist_select)) {

            LOGGER.info("Track artists: {}", track.getArtists());

            for (String artist : track.getArtists()) {
                if (artist == null || artist.isBlank()) {
                    continue;
                }

                LOGGER.info("Inserting artist: {}", artist);
                artist_stmt.setString(1, artist);
                artist_stmt.executeUpdate();

                artist_select_stmt.setString(1, artist);

                try (ResultSet rs = artist_select_stmt.executeQuery()) {
                    if (rs.next()) {
                        artist_ids.add(rs.getLong("id"));
                    } else {
                        throw new SQLException("Could not retrieve artist ID");
                    }
                }
            }
        }

        String artist_song = """
                INSERT OR IGNORE INTO artist_song (
                    artist_id,
                    song_id
                )
                VALUES (?, ?)
                """;
        
        try (PreparedStatement artist_song_stmt = conn.prepareStatement(artist_song)) {
            for (long artist_id : artist_ids) {
                artist_song_stmt.setLong(1, artist_id);
                artist_song_stmt.setLong(2, track_id);
                artist_song_stmt.executeUpdate();
            }
        }


        return track_id;

        } catch (IOException | SQLException e) {
            LOGGER.error("Failed to insert track {}", track.getFilePath(), e);
            throw new RuntimeException(e);
        }
    }

    public long insertAlbum(AlbumMetadata album) {

        if (conn == null) {
            openConnection();
        }

        String album_sql = """
            INSERT INTO album (
                name,
                folder_path,
                cover_path,
                length,
                release_date,
                discs
            )
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = conn.prepareStatement(album_sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, album.getName());
            stmt.setString(2, album.getFolderPath().toString());

            if (album.getCoverPath() != null) {
                stmt.setString(3, album.getCoverPath().toString());
            } else {
                stmt.setNull(3, Types.VARCHAR);
            }

            stmt.setInt(4, album.getLengthSeconds());
            stmt.setString(5, album.getReleaseDate());
            stmt.setInt(6, album.getDiscCount());

            stmt.executeUpdate();

            long album_id = -1;

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    album_id = keys.getLong(1);
                }
            }

            if (album_id == -1) {
                throw new SQLException("No album ID returned.");
            }

            String artist_sql = """
            INSERT INTO artist (
                name
            )
            VALUES (?)
            ON CONFLICT(name) DO NOTHING
            """;
        
        List<Long> artist_ids = new ArrayList<>();

        try (PreparedStatement artist_stmt = conn.prepareStatement(artist_sql, Statement.RETURN_GENERATED_KEYS)) {

            LOGGER.info("Album artists: {}", album.getAlbumArtists());
            for (String artist : album.getAlbumArtists()) {
                LOGGER.info("Inserting artist: {}", artist);
                artist_stmt.setString(1, artist);

                artist_stmt.executeUpdate();

                try (ResultSet keys = artist_stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        artist_ids.add(keys.getLong(1));
                    }
                }
            }
        }

        String album_artist = """
                INSERT OR IGNORE INTO album_artist (
                    album_id,
                    artist_id
                )
                VALUES (?, ?)
                """;
        
        try (PreparedStatement album_artist_stmt = conn.prepareStatement(album_artist)) {
            for (long artist_id : artist_ids) {
                album_artist_stmt.setLong(1, album_id);
                album_artist_stmt.setLong(2, artist_id);
                album_artist_stmt.executeUpdate();
            }
        }

        return album_id;

        } catch (SQLException e) {
            LOGGER.error("Failed to insert album {}", album.getFolderPath(), e);
            throw new RuntimeException(e);
        }
    }
}
