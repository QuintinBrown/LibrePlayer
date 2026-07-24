package com.QuintinBrown.LibrePlayer.playback;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class AudioOutput {
    private SourceDataLine line;

    public void open(AudioFormat format) throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        line = (SourceDataLine) AudioSystem.getLine(info);

        line.open(format);
        line.start();
    }

    public void write(byte[] data, int offset, int length) {
        line.write(data, offset, length);
    }
}
