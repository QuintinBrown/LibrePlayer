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

    private static Path getApplicationDirectory() {
        try {
            Path location = Paths.get(
                AppDirectories.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
            );

            if (Files.isRegularFile(location)) {
                return location.getParent();
            }

            return location;

        } catch (URISyntaxException e) {
            throw new RuntimeException("Unable to determine application directory", e);
        }
    }

    public static void initialize() throws IOException {
        logger.info("Creating directories");
        Files.createDirectories(ROOT.resolve("themes"));
        Files.createDirectories(ROOT.resolve("logs"));
        Files.createDirectories(ROOT.resolve("database"));
        logger.info("Finished creating directories");
    }

    public static Path themeDir() {return THEMES;}

    public static Path logsDir() {return LOGS;}

    public static Path databaseDir() {return DATABASE;}
}
