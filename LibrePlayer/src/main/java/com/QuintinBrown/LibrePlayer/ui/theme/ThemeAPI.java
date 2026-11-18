package com.QuintinBrown.LibrePlayer.ui.theme;

import java.io.IOException;

public class ThemeAPI {

    private final ThemeManager themeManager;

    public ThemeAPI(ThemeManager themeManager) {
        this.themeManager = themeManager;
    }

    public String[] getAvailableThemes() {
        try {
            return themeManager.getAvailableThemeNames().toArray(new String[0]);
        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve themes", e);
        }
    }

    public String getCurrentTheme(){return themeManager.getCurrentTheme();}

    public void setTheme(String themeName) {
        try {
            themeManager.setTheme(themeName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load theme: " + themeName, e);
        }
    }
}
