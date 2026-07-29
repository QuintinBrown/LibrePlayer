package com.QuintinBrown.LibrePlayer.playback;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Player {
    private static final Logger LOGGER = LoggerFactory.getLogger(Player.class);

    private final Decoder decoder;
    private final AudioOutput output;

    private Thread playbackThread;

    private PlaybackState state = PlaybackState.STOPPED;

    private byte[] buffer;

    private volatile boolean running;
    private volatile boolean paused;

    public Player() {
        this.decoder = new Decoder();
        this.buffer = new byte[4096];
        this.output = new JavaSoundOutput();
    }

    private void playbackLoop() {
        buffer = new byte[8192];

        try {
            while (running) {
                if (paused) {
                    Thread.sleep(10);
                    continue;
                }

                int bytes = decoder.read(buffer);

                if (bytes == -1) {
                    break;
                }

                output.write(buffer, 0, bytes);
            }
        } catch (Exception e) {
            LOGGER.error("Error in UI: " + e);
        } finally {
            output.flush();
            state = PlaybackState.STOPPED;
            running = false;
        }
    }

    public void open(Path source) {
        if (state == PlaybackState.PLAYING) {
            pause();
        }

        decoder.open(source);

        output.open(decoder.getOutputFormat());

        LOGGER.info("Opened file at: " + source.toString());
    }

    public synchronized void play() {
        //if (currentTrack == null) {
        //    throw new IllegalStateException("No track loaded");
        //}

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

        playbackThread = new Thread(this::playbackLoop, "Playback Thread");

        playbackThread.start();

        state = PlaybackState.PLAYING;

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
}
