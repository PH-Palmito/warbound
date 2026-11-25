package com.warbound.ui.components;

import com.warbound.ui.fx.Efeitos;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * Janela de confirmação reutilizável (ex.: "Sair da partida?"). Usar como
 * camada por cima da tela atual: adicionar a um StackPane e remover ao
 * confirmar ou cancelar.
 */
public class ModalConfirmacao extends StackPane {

    public ModalConfirmacao(String titulo, String mensagem, Runnable aoConfirmar, Runnable aoCancelar) {
        getStyleClass().add("modal-overlay");
        setAlignment(Pos.CENTER);

        Label tituloLabel = new Label(titulo);
        tituloLabel.getStyleClass().add("modal-title");

        Label mensagemLabel = new Label(mensagem);
        mensagemLabel.getStyleClass().add("text-body");
        mensagemLabel.setWrapText(true);
        mensagemLabel.setTextAlignment(TextAlignment.CENTER);
        mensagemLabel.setMaxWidth(340);

        SecondaryButton cancelar = new SecondaryButton("Cancelar", aoCancelar);
        PrimaryButton confirmar = new PrimaryButton("Confirmar", aoConfirmar);
        HBox botoes = new HBox(10, cancelar, confirmar);
        botoes.setAlignment(Pos.CENTER);

        VBox caixa = new VBox(16, tituloLabel, mensagemLabel, botoes);
        caixa.setAlignment(Pos.CENTER);
        caixa.setMaxWidth(400);
        caixa.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        caixa.getStyleClass().add("modal-box");

        getChildren().add(caixa);
        Efeitos.aparecer(this, 160);
        Efeitos.abrirModal(caixa);
    }
}
