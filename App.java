package com.votreport; // Ligne cruciale en haut du fichier

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/votreport/views/acc.fxml")); 
        Scene scene = new Scene(root);
        stage.setTitle("Gestion des affectations des employés");
        stage.setScene(scene);
        stage.setMaximized(true);  // plein écran
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);  // Cette ligne est cruciale
    }
}


