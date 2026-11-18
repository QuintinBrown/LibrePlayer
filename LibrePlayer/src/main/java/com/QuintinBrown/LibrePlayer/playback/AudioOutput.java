package com.QuintinBrown.LibrePlayer.playback;

import javax.sound.sampled.AudioFormat;

public interface AudioOutput {
    void open(AudioFormat format);

    void start();

    void write(byte[] data, int offset, int length);

    void pause();

    void resume();

    void flush();

    void close();

    void stop();

    boolean isOpen();
}
