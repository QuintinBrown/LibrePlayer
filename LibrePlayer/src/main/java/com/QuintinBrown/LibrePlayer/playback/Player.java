package com.QuintinBrown.LibrePlayer.playback;

import java.nio.file.Path;

public class Player {
    private final Decoder decoder;
    private final AudioOutput output;

    private Thread playbackThread;

    private Track currentTrack;

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
            e.printStackTrace();
        } finally {
            output.flush();
            state = PlaybackState.STOPPED;
            running = false;
        }
    }

    public void open(Path source) {
        decoder.open(source);

        output.open(decoder.getOutputFormat());

        output.start();
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
    }

    public synchronized void pause() {
        if (state != PlaybackState.PLAYING) {
            return;
        }

        paused = true;

        output.pause();

        state = PlaybackState.PAUSED;
    }
}
