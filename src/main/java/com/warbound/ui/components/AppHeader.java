package com.warbound.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Cabeçalho comum a todas as telas: nome do jogo, título da tela e, quando
 * aplicável, botão de voltar. A faixa fina na base repete as três cores
 * das facções (amarelo, vermelho e azul).
 */
public class AppHeader extends VBox {

    public AppHeader(String tituloDaTela, Runnable aoVoltar, boolean mostrarLogo) {
        getStyleClass().add("app-header");

        HBox linha = new HBox(14);
        linha.setAlignment(Pos.CENTER_LEFT);
        linha.setPadding(new Insets(8, 18, 8, 18));

        if (mostrarLogo) {
            ImagePlaceholder nome = new ImagePlaceholder("/assets/logo/warbound_wordmark.png",
                    150, 29, "Warbound", ImagePlaceholder.Modo.CONTER, 0.5, false);
            Region divisor = new Region();
            divisor.getStyleClass().add("header-divider");
            linha.getChildren().addAll(nome, divisor);
        }

        Label titulo = new Label(tituloDaTela);
        titulo.getStyleClass().add("screen-title");

        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);

        linha.getChildren().addAll(titulo, espaco);

        if (aoVoltar != null) {
            linha.getChildren().add(new SecondaryButton("« Voltar", aoVoltar));
        }

        Region faixa = new Region();
        faixa.getStyleClass().add("faction-strip");
        faixa.setMinHeight(3);
        faixa.setPrefHeight(3);
        faixa.setMaxHeight(3);

        getChildren().addAll(linha, faixa);
    }

    public AppHeader(String tituloDaTela, Runnable aoVoltar) {
        this(tituloDaTela, aoVoltar, true);
    }

    public AppHeader(String tituloDaTela) {
        this(tituloDaTela, null, true);
    }
}
