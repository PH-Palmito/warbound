package com.warbound.ui.components;

import javafx.scene.control.Label;

/**
 * Selo de estado (turno, protegida, com Fúria, carregando...). Cor nunca é
 * o único indicador: cada tipo traz ícone e palavra, conforme o guia de
 * frontend.
 */
public class StatusBadge extends Label {

    public enum Tipo {
        TURNO("⏳", "Turno", "status-badge-turn"),
        CARREGANDO("⌛", "Carregando", "status-badge-charging"),
        PROTEGIDA("🛡", "Protegida", "status-badge-protected"),
        FURIA("🔥", "Fúria", "status-badge-fury"),
        RESERVA("💤", "Reserva", "status-badge-reserve"),
        DANO("⚔", "Dano", "status-badge-damage"),
        CURA("✚", "Cura", "status-badge-heal"),
        BUFF("★", "Buff de grupo", "status-badge-buff"),
        PODE_ATACAR("⚔", "Pode atacar", "status-badge-ready"),
        JA_ATACOU("✔", "Já atacou", "status-badge-done"),
        SELECIONADA("◎", "Selecionada", "status-badge-selected");

        final String icone;
        final String texto;
        final String styleClass;

        Tipo(String icone, String texto, String styleClass) {
            this.icone = icone;
            this.texto = texto;
            this.styleClass = styleClass;
        }
    }

    public StatusBadge(Tipo tipo) {
        this(tipo, tipo.texto);
    }

    /** Permite customizar o texto (ex.: "Carregando (1)"), mantendo ícone e cor do tipo. */
    public StatusBadge(Tipo tipo, String textoCustom) {
        super(tipo.icone + " " + textoCustom);
        getStyleClass().addAll("status-badge", tipo.styleClass);
    }
}
