package com.warbound.ui.components;

import com.warbound.ui.audio.Sons;
import javafx.scene.control.Button;

/** Botão de ação principal (ex.: Jogar, Criar partida, Encerrar turno). */
public class PrimaryButton extends Button {

    public PrimaryButton(String texto, Runnable aoClicar) {
        this(texto, aoClicar, true);
    }

    /** Com somDeClique = false, o botão não toca o clique genérico (a ação já tem som próprio). */
    public PrimaryButton(String texto, Runnable aoClicar, boolean somDeClique) {
        super(texto);
        getStyleClass().add("primary-button");
        setOnAction(e -> {
            if (somDeClique) Sons.tocar(Sons.Som.CLIQUE);
            if (aoClicar != null) aoClicar.run();
        });
    }
}
