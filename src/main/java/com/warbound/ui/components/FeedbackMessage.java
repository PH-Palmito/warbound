package com.warbound.ui.components;

import com.warbound.ui.fx.Efeitos;
import javafx.scene.control.Label;

/**
 * Linha de mensagem de resultado ou erro, mostrada após uma ação. O
 * frontend só exibe o texto que o servidor (ou o GameClientMock) devolveu.
 */
public class FeedbackMessage extends Label {

    public enum Tipo { INFO, SUCESSO, ERRO }

    public FeedbackMessage(String texto, Tipo tipo) {
        super(texto);
        setWrapText(true);
        getStyleClass().add("feedback-message");
        switch (tipo) {
            case SUCESSO: getStyleClass().add("feedback-message-success"); break;
            case ERRO: getStyleClass().add("feedback-message-error"); break;
            default: getStyleClass().add("feedback-message-info"); break;
        }
        Efeitos.aparecer(this, 240);
    }
}
