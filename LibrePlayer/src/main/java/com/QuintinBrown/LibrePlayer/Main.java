package com.QuintinBrown.LibrePlayer;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.QuintinBrown.LibrePlayer.playback.AudioOutput;
import com.QuintinBrown.LibrePlayer.playback.Decoder;
import com.QuintinBrown.LibrePlayer.playback.JavaSoundOutput;

/**
 * Hello world!
 *
 */
public class Main 
{
    public static void main( String[] args ) throws Exception {
        Decoder decoder = new Decoder();
        Path path = Paths.get("A_FILE.mp3");

        decoder.open(path);

        AudioOutput output = new JavaSoundOutput();

        output.open(decoder.getOutputFormat());

        output.start();

        byte[] buffer = new byte[4096];

        while (true) { 
            int bytes = decoder.read(buffer);

            if (bytes == -1) {
                break;
            }

            output.write(buffer, 0, bytes);
        }

        output.close();
        decoder.close();
    }
}
