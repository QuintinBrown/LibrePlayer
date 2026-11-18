package com.QuintinBrown.LibrePlayer;

import java.nio.file.Files;

import com.QuintinBrown.LibrePlayer.library.AppDirectories;
import com.QuintinBrown.LibrePlayer.library.DatabaseManager;
import com.QuintinBrown.LibrePlayer.ui.App;

import javafx.application.Application;

public class Main 
{
    public static void main( String[] args ) throws Exception {  
        
        AppDirectories.initialize();

        if (!Files.exists(AppDirectories.databaseDir().resolve("library.db"))) {
            DatabaseManager.initializeDB();
        }

        Application.launch(App.class, args);
    }

}
