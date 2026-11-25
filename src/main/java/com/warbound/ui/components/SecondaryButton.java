package com.warbound.ui.components;

import com.warbound.ui.audio.Sons;
import javafx.scene.control.Button;

/** Botão de ação secundária (ex.: Voltar, Atualizar lobby). */
public class SecondaryButton extends Button {

    public SecondaryButton(String texto, Runnable aoClicar) {
        this(texto, aoClicar, true);
    }

    /** Com somDeClique = false, o botão não toca o clique genérico (a ação já tem som próprio). */
    public SecondaryButton(String texto, Runnable aoClicar, boolean somDeClique) {
        super(texto);
        getStyleClass().add("secondary-button");
        setOnAction(e -> {
            if (somDeClique) Sons.tocar(Sons.Som.CLIQUE);
            if (aoClicar != null) aoClicar.run();
        });
    }
}
