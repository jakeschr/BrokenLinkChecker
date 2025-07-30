package com.unpar.brokenlinkchecker;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Application extends javafx.application.Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("views/main.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        // Buat stage tanpa title bar bawaan OS
        // stage.initStyle(StageStyle.UNDECORATED);

        stage.setTitle("BrokenLink Checker");
        stage.setScene(scene);

        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setMaximized(true);  // Atau bisa dihapus kalau tidak ingin fullscreen

        stage.show();


    // https://informatika.unpar.ac.id
    }

    public static void main(String[] args) {
        launch();
    }
}
