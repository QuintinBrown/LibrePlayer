package com.QuintinBrown.LibrePlayer.library;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppDirectories {
    private static final Logger logger = LoggerFactory.getLogger(AppDirectories.class);

    private static final Path ROOT = getApplicationDirectory();
    private static final Path THEMES = ROOT.resolve("themes");
    private static final Path LOGS = ROOT.resolve("logs");
    private static final Path DATABASE = ROOT.resolve("database");
    private static final Path IMAGES = ROOT.resolve("images");
    private static final Path ALBUM_COVERS = IMAGES.resolve("covers");

    private static Path getApplicationDirectory() {

        logger.info("Portable mode: {}", Boolean.getBoolean("app.portable"));
        logger.info("Executable: {}", ProcessHandle.current().info().command().orElse("Unknown"));

        String executable = ProcessHandle.current().info().command().orElseThrow(() -> 
                                                                            new IllegalStateException("Unable to locate app executable"));

        if (Boolean.getBoolean("app.portable")) {
            if (executable.equals("Unknown")) {
                throw new IllegalStateException("Unable to locate app executable");
            }
                    
            Path executablePath = Paths.get(executable).toAbsolutePath();

            Path directory = executablePath.getParent();

            logger.info("Application root: {}", directory);

            return directory;
        }

        // JAR execution
        try {
            Path location = Paths.get(AppDirectories.class.getProtectionDomain().getCodeSource().getLocation().toURI());

            if (Files.isRegularFile(location)) {
                return location.getParent();
            }

        } catch (URISyntaxException e) {
            throw new RuntimeException("Unable to determine application directory", e);
        }

        // Fallback to current working directory
        return Paths.get("").toAbsolutePath();
    }

    public static void initialize() throws IOException {
        logger.info("Creating directories");
        Files.createDirectories(ROOT.resolve("themes"));
        Files.createDirectories(ROOT.resolve("logs"));
        Files.createDirectories(ROOT.resolve("database"));
        Files.createDirectories(ROOT.resolve("images"));
        Files.createDirectories(IMAGES.resolve("album_covers"));
        logger.info("Finished creating directories");
    }

    public static Path themeDir() {return THEMES;}

    public static Path logsDir() {return LOGS;}

    public static Path databaseDir() {return DATABASE;}

    public static Path imagesDir() {return IMAGES;}

    public static Path coversDir() {return ALBUM_COVERS;}
}
