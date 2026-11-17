package com.QuintinBrown.LibrePlayer.ui.theme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.QuintinBrown.LibrePlayer.library.AppDirectories;

public class ThemeManager {
    private final Path themesDirectory;

    public ThemeManager() {
        this.themesDirectory = AppDirectories.themeDir();
    }

    public List<Path> getAvailableThemes() throws IOException {
        List<Path> themes = new ArrayList<>();

        themes.addAll(findThemes(themesDirectory));
        return themes;
    }

    private List<Path> findThemes(Path directory) throws IOException {
        List<Path> themes = new ArrayList<>();

        try (var paths = Files.list(directory)) {
            paths.filter(Files::isDirectory).filter(path -> Files.exists(path.resolve("index.html"))).forEach(themes::add);
        }

        return themes;
    }
}
