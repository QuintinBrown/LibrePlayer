package com.QuintinBrown.LibrePlayer.library;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public long insertAlbum(AlbumExtractionMetadata album) {

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

    public TrackMetadata getTrack(long id) {
        LOGGER.info("Starting track lookup for track: {}", id);
        String select_sql = """
            SELECT *
            from song
            WHERE id = ?
                """;

        String select_artists = """
            SELECT artist.name
            FROM artist
            JOIN artist_song on artist_song.artist_id = artist.id
            WHERE artist_song.song_id = ?;
                """;

        try (PreparedStatement select_sql_stmt = conn.prepareStatement(select_sql);
             PreparedStatement select_artists_stmt = conn.prepareStatement(select_artists)) {

            LOGGER.info("getTrack: Prepared statement created");

            select_artists_stmt.setLong(1, id);
            select_sql_stmt.setLong(1, id);

            LOGGER.info("getTrack: parameters assigned");

            try (ResultSet rs = select_sql_stmt.executeQuery()) {

                LOGGER.info("getTrack: Song query executed");

                List<String> artists = new ArrayList<>();
                try (ResultSet artists_rs = select_artists_stmt.executeQuery()) {
                    LOGGER.info("getTrack: Artist query executed");
                    while (artists_rs.next()) {
                        artists.add(artists_rs.getString("name"));
                    }
                }

                LOGGER.info("getTrack: Found {} artists", artists.size());

                if (rs.next()) {
                    LOGGER.info("getTrack: Found song, constructing metadata");
                    LOGGER.info("Title: {}", rs.getString("title"));
                    LOGGER.info("Filepath: {}", Paths.get(rs.getString("filepath")));
                    LOGGER.info("Length: {}", rs.getInt("length"));
                    LOGGER.info("Track number: {}", rs.getInt("track_number"));
                    LOGGER.info("Disc number: {}", rs.getInt("disc_number"));
                    LOGGER.info("Channels: {}", rs.getInt("channels"));
                    LOGGER.info("Sample rate: {}", rs.getInt("sample_rate"));
                    LOGGER.info("Bitrate: {}", rs.getInt("bitrate"));
                    LOGGER.info("Codec: {}", rs.getString("codec"));
                    LOGGER.info("Artists: {}", artists);
                    TrackMetadata track = new TrackMetadata(rs.getString("title"), Paths.get(rs.getString("filepath")), rs.getInt("length"), rs.getInt("track_number"), rs.getInt("disc_number"), rs.getInt("channels"), rs.getInt("sample_rate"), rs.getInt("bitrate"), rs.getString("codec"), artists, null);
                    LOGGER.info("getTrack: Metadata constructed successfully");
                    return track;
                } else {
                    throw new SQLException("Could not find track with id: " + id);
                }
            }
                
        } catch (SQLException e) {
            LOGGER.error("Failed to return track {}", id, e);
            throw new RuntimeException(e);    
        }
    }

    public AlbumMetadata getAlbum(long albumID) {
        String select_sql = """
            SELECT *
            from album
            WHERE id = ?
                """;

        String select_artists = """
            SELECT artist.name
            FROM artist
            JOIN album_artist on album_artist.artist_id = artist.id
            WHERE album_artist.album_id = ?
                """;

        String select_tracks = """
            SELECT id
            FROM song
            WHERE album_id = ?
            ORDER BY disc_number ASC, track_number ASC, id ASC
                """;

        try (PreparedStatement select_sql_stmt = conn.prepareStatement(select_sql);
             PreparedStatement select_artists_stmt = conn.prepareStatement(select_artists);
             PreparedStatement select_tracks_stmt = conn.prepareStatement(select_tracks)) {

            select_artists_stmt.setLong(1, albumID);
            select_sql_stmt.setLong(1, albumID);
            LOGGER.info("Getting tracks with album ID {}", albumID);
            select_tracks_stmt.setLong(1, albumID);

            try (ResultSet rs = select_sql_stmt.executeQuery()) {

                List<String> artists = new ArrayList<>();
                try (ResultSet artists_rs = select_artists_stmt.executeQuery()) {
                    while (artists_rs.next()) {
                        artists.add(artists_rs.getString("name"));
                    }
                }

                List<Long> tracks = new ArrayList<>();
                try (ResultSet tracks_rs = select_tracks_stmt.executeQuery()) {
                    while (tracks_rs.next()) {
                        tracks.add(tracks_rs.getLong("id"));
                    }
                }

                if (rs.next()) {
                    LOGGER.info("Database retrieved {} track IDs: {}", tracks.size(), tracks);
                    AlbumMetadata album = new AlbumMetadata(rs.getString("name"), Paths.get(rs.getString("folder_path")), Paths.get(rs.getString("cover_path")), rs.getString("release_date"), rs.getInt("length"), rs.getInt("discs"), tracks, artists, null);
                    return album;
                } else {
                    throw new SQLException("Could not find album with id: " + albumID);
                }
            }
                
        } catch (SQLException e) {
            LOGGER.error("Failed to return tracks from {}", albumID, e);
            throw new RuntimeException(e);    
        }
    }

    public List<TrackMetadata> getTracksFromAlbum(long albumID) {
        String select_sql = """
            SELECT id
            FROM song
            WHERE song.album_id = ?
            ORDER BY disc_number ASC, track_number ASC, id ASC
                """;

        try (PreparedStatement select_stmt = conn.prepareStatement(select_sql)) {
            select_stmt.setLong(1, albumID);

            List<TrackMetadata> tracks = new ArrayList<>();
            try (ResultSet rs = select_stmt.executeQuery()) {
                while (rs.next()) {
                    TrackMetadata track = getTrack(rs.getLong("id"));
                    tracks.add(track);
                }
                return tracks;
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to return tracks from album: ", albumID, e);
            throw new RuntimeException(e);
        }
    }

    public Map<Long, AlbumMetadata> getAllAlbums() {
        String select_sql = """
            SELECT id
            FROM album
                """;

        try (PreparedStatement select_stmt = conn.prepareStatement(select_sql)) {
            Map<Long, AlbumMetadata> albums = new HashMap<>();
            try (ResultSet rs = select_stmt.executeQuery()) {
                while (rs.next()) {
                    AlbumMetadata albumData = getAlbum(rs.getLong("id"));
                    albums.put(rs.getLong("id"), albumData);
                }
                return albums;
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to retrieve albums");
            throw new RuntimeException(e);
        }
    }

    public int getTrackCount(Long albumID) {
        String count_sql = """
            SELECT COUNT(ID) AS track_count
            FROM song
            WHERE album_id = ?
            """;

        try (PreparedStatement count_stmt = conn.prepareStatement(count_sql)) {

            count_stmt.setLong(1, albumID);

            try (ResultSet rs = count_stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("track_count");
                }
                return 0;
            }

        } catch (SQLException e) {
            LOGGER.error("Failed to retrieve track count for album {}", albumID, e);
            throw new RuntimeException(e);
        }
    }
}
