package com.unpar.brokenlinkchecker;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Application extends javafx.application.Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        stage.setTitle("BrokenLink Checker");
        stage.setScene(scene);

        stage.setMinWidth(900);   // lebar minimum
        stage.setMinHeight(600);  // tinggi minimum
        stage.setMaximized(true); // tampil fullscreen saat mulai

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
