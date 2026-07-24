package com.QuintinBrown.LibrePlayer.playback;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class JavaSoundOutput implements AudioOutput {
    private SourceDataLine line;
    private AudioFormat format;
    private boolean paused;

    @Override
    public void open(AudioFormat format) {
        this.format = format;

        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            line = (SourceDataLine) AudioSystem.getLine(info);

            line.open(format);

        } catch (LineUnavailableException e) {
            throw new RuntimeException("Failed to open audio device " + e);
        }
    }

    @Override
    public void start() {
        if (line == null) {
            throw new IllegalStateException("Output not opened");
        }

        line.start();
        paused = false;
    }

    @Override
    public void write(byte[] data, int offset, int length) {
        if (line == null) {
            throw new IllegalStateException("Output not opened");
        }

        if (!paused) {
            line.write(data, offset, length);
        }
    }

    @Override
    public void pause() {
        if (line != null) {
            line.stop();
            paused = true;
        }
    }

    @Override
    public void resume() {
        if (line != null) {
            line.start();
            paused = false;
        }
    }

    @Override
    public void flush() {
        if (line != null) {
            line.flush();
        }
    }

    @Override
    public boolean isOpen() {
        return line != null && line.isOpen();
    }

    @Override
    public void close() {
        if (line != null) {
            line.stop();
            line.close();

            line = null;
        }
    }
}
