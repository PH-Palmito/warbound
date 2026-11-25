package com.warbound;

import com.warbound.ui.ScreenManager;
import com.warbound.ui.fx.Recursos;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        Recursos.carregarFontes();   // antes de criar a Scene: o CSS usa a MedievalSharp
        com.warbound.ui.audio.Sons.diagnosticar();   // mostra no console se o Java enxerga a saída de áudio

        stage.setTitle("Warbound");
        Image icone = Recursos.imagem("/assets/logo/warbound_icon.png");
        if (icone != null) {
            stage.getIcons().add(icone);
        }

        ScreenManager screenManager = new ScreenManager(stage);
        screenManager.mostrarMenu();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
