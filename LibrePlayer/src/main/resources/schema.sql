CREATE TABLE IF NOT EXISTS song (
    id INTEGER PRIMARY KEY,
    title TEXT,
    length INTEGER, --length in seconds
    filepath TEXT UNIQUE,
    playcount INTEGER,
    last_listen INTEGER, --file timestamps in UNIX epoch seconds
    first_added INTEGER,
    last_modified INTEGER,
    file_size INTEGER,
    disc_number INTEGER,
    channels INTEGER,
    container TEXT,
    codec TEXT,
    sample_rate INTEGER,
    bitrate INTEGER,
    isFavorite INTEGER NOT NULL DEFAULT 0,
    album_id INTEGER,
    track_number INTEGER
);

CREATE TABLE IF NOT EXISTS artist (
    id INTEGER PRIMARY KEY,
    name TEXT UNIQUE,
    profile_photo_path TEXT,
    description TEXT,
    logo_photo_path TEXT
);

CREATE TABLE IF NOT EXISTS album (
    id INTEGER PRIMARY KEY,
    name TEXT,
    folder_path TEXT UNIQUE,
    cover_path TEXT,
    length INTEGER,
    release_date TEXT, --YYYY-MM-DD
    discs INTEGER,
    track_count INTEGER
);

CREATE TABLE IF NOT EXISTS playlist (
    id PRIMARY KEY,
    name TEXT,
    cover_path TEXT,
    length INTEGER,
    created INTEGER,
    modified INTEGER
);

CREATE TABLE IF NOT EXISTS genre (
    id PRIMARY KEY,
    name TEXT
);

CREATE TABLE IF NOT EXISTS album_artist (
    album_id INTEGER,
    artist_id INTEGER,
    PRIMARY KEY (album_id, artist_id)
);

CREATE TABLE IF NOT EXISTS album_genre (
    album_id INTEGER,
    genre_id INTEGER,
    PRIMARY KEY (album_id, genre_id)
);

CREATE TABLE IF NOT EXISTS artist_song (
    artist_id INTEGER,
    song_id INTEGER,
    PRIMARY KEY (artist_id, song_id)
);

CREATE TABLE IF NOT EXISTS playlist_song (
    playlist_id INTEGER,
    song_id INTEGER,
    PRIMARY KEY (playlist_id, song_id)
);

CREATE TABLE IF NOT EXISTS song_genre (
    song_id INTEGER,
    genre_id INTEGER,
    PRIMARY KEY (song_id, genre_id)
);