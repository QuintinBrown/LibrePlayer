package com.QuintinBrown.LibrePlayer.library;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

public class AlbumArtworkGenerator {
    public Path generate(Long albumID, AlbumExtractionMetadata album) {
        List<TrackMetadata> tracks = album.getTracks();

        if (tracks.isEmpty()) {
            return null;
        }

        Path outputDir = AppDirectories.coversDir();
        
        try {
            Files.createDirectories(outputDir);
            
            for (TrackMetadata track : tracks) {
                AudioFile file = AudioFileIO.read(track.getFilePath().toFile());

                Tag tag = file.getTag();

                if (tag == null) {
                    continue;
                }

                Artwork artwork = tag.getFirstArtwork();

                if (artwork == null) {
                    continue;
                }

                byte[] imageData = artwork.getBinaryData();

                if (imageData == null || imageData.length == 0) {
                    continue;
                }

                String extension = getExtension(artwork.getMimeType());

                Path outputPath = outputDir.resolve(albumID + extension);

                Files.write(outputPath, imageData);

                return outputPath;
            }

            return null;

        } catch (Exception e) {
            return null;
        }
    }

    private String getExtension(String mimeType) {

        if (mimeType.toLowerCase().equals("image/png")) {
            return ".png";
        } else if (mimeType.toLowerCase().equals("image/webp")) {
            return ".webp";
        } else {
            return ".jpg";
        }
    }
}
