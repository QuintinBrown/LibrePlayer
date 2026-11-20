package com.QuintinBrown.LibrePlayer.playback;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.QuintinBrown.LibrePlayer.library.DatabaseManager;
import com.QuintinBrown.LibrePlayer.library.TrackMetadata;

public class Player implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Player.class);

    private final Decoder decoder;
    private final AudioOutput output;

    private DatabaseManager manager;

    private Thread playbackThread;

    private volatile PlaybackState state = PlaybackState.STOPPED;

    private byte[] buffer;

    private volatile boolean running;
    private volatile boolean paused;
    private volatile boolean closed;

    private Runnable onTrackFinish;

    public Player(DatabaseManager manager) {
        this.decoder = new Decoder();
        this.buffer = new byte[8192];
        this.output = new JavaSoundOutput();
        this.manager = manager;
    }

    private void playbackLoop() {
        boolean completedNaturally = false;

        try {
            while (running) {
                if (paused) {
                    Thread.sleep(10);
                    continue;
                }

                int bytes = decoder.read(buffer);

                if (bytes == -1) {
                    completedNaturally = true;
                    break;
                }

                output.write(buffer, 0, bytes);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
    
        } catch (Exception e) {
            if (running) {
                LOGGER.error("Error in UI: " + e);
            }
        } finally {
            running = false;
            paused = false;
            
            synchronized (this) {
                if (playbackThread == Thread.currentThread()) {
                    playbackThread = null;
                }

                state = PlaybackState.STOPPED;
            }
            
            LOGGER.info("Playback thread terminated");

            if (completedNaturally && onTrackFinish != null) {
                try {
                    onTrackFinish.run();
                } catch (Exception e) {
                    LOGGER.error("Error handling track completion", e);
                }
            }
        }
    }

    public void open(Path source) {
        ensureOpen();

        stop();

        decoder.open(source);

        output.open(decoder.getOutputFormat());

        LOGGER.info("Opened file at: {}", source);
    }

    public void open(Long trackID) {
        LOGGER.info("Attempting to open track with id: {}", trackID);

        LOGGER.info("Calling manager.getTrack()");
        TrackMetadata track = manager.getTrack(trackID);
        LOGGER.info("manager.getTrack() returned");

        if (track == null) {
            LOGGER.error("No track metadata found for track: {}", trackID);
            throw new IllegalStateException("Null track metadata");
        }

        LOGGER.info("Retrieved track metadata {}", track);

        Path path = track.getFilePath();
        LOGGER.info("Attempting to open track at path {}", path);

        open(path);
    }

    public synchronized void play() {
        ensureOpen();

        if (!decoder.isInitialized()) {
            throw new IllegalStateException("No file loaded");
        }

        if (state == PlaybackState.PLAYING) {
            return;
        }

        if (state == PlaybackState.PAUSED) {
            paused = false;
            output.resume();
            state = PlaybackState.PLAYING;
            return;
        }

        running = true;
        paused = false;

        output.start();

        state = PlaybackState.PLAYING;

        playbackThread = new Thread(this::playbackLoop, "Playback Thread");

        playbackThread.start();

        LOGGER.info("Started playback");
    }

    public synchronized void pause() {
        if (state != PlaybackState.PLAYING) {
            return;
        }

        paused = true;

        output.pause();

        state = PlaybackState.PAUSED;

        LOGGER.info("Playback paused");
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }

        LOGGER.info("Closing player");

        running = false;
        paused = false;

        Thread thread = playbackThread;

        output.close();

        if (thread != null && thread != Thread.currentThread()) {
            thread.interrupt();

            try {
                thread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        decoder.close();

        playbackThread = null;
        state = PlaybackState.STOPPED;
        closed = true;

        LOGGER.info("Player closed");
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Player is closed");
        }
    }

    public synchronized void stop() {
        running = false;
        paused = false;

        Thread thread = playbackThread;

        if (thread != null && thread != Thread.currentThread()) {
            output.stop();
            thread.interrupt();

            try {
                thread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        } else {
            output.stop();
        }

        state = PlaybackState.STOPPED;
        LOGGER.info("Playback stopped");
    }

    public void setOnTrackFinished(Runnable callback) {
        this.onTrackFinish = callback;
    }

    public PlaybackState getState() {return state;}
}
