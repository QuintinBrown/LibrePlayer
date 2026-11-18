package com.QuintinBrown.LibrePlayer.ui.theme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.QuintinBrown.LibrePlayer.library.AppDirectories;

import javafx.scene.web.WebEngine;

public class ThemeManager {
    private final Path themesDirectory;
    private final WebEngine webEngine;
    private Path currentTheme;

    public ThemeManager(WebEngine webEngine) {
        this.themesDirectory = AppDirectories.themeDir();
        this.webEngine = webEngine;
    }

    public List<Path> getAvailableThemes() throws IOException {
        List<Path> themes = new ArrayList<>();

        themes.addAll(findThemes(themesDirectory));
        return themes;
    }

    public List<String> getAvailableThemeNames() throws IOException {
        List<String> names = new ArrayList<>();

        for (Path theme : getAvailableThemes()) {
            names.add(theme.getFileName().toString());
        }

        return names;
    }

    private List<Path> findThemes(Path directory) throws IOException {
        List<Path> themes = new ArrayList<>();

        if (!Files.isDirectory(directory)) {
            return themes;
        }

        try (var paths = Files.list(directory)) {
            paths.filter(Files::isDirectory).filter(path -> Files.exists(path.resolve("index.html"))).forEach(themes::add);
        }

        return themes;
    }

    public void setTheme(String themeName) throws IOException {
        Path selectedTheme = getAvailableThemes().stream()
                                                 .filter(path -> path.getFileName()
                                                 .toString()
                                                 .equals(themeName))
                                                 .findFirst()
                                                 .orElseThrow(() -> new IllegalArgumentException("Theme not found: " + themeName));

        loadTheme(selectedTheme);
    }

    private void loadTheme(Path themeDirectory) {
        Path htmlFile = themeDirectory.resolve("index.html");

        if (!Files.isRegularFile(htmlFile)) {
            throw new IllegalArgumentException("Theme does not contain index.html");
        }

        webEngine.load(htmlFile.toUri().toString());

        currentTheme = themeDirectory;
    }

    public String getCurrentTheme() {
        if (currentTheme == null) {
            return null;
        }

        return currentTheme.getFileName().toString();
    }

    public Path getCurrentThemePath() {return currentTheme;}
}
