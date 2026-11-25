package com.warbound.shared.dto;

import java.io.Serializable;
import java.util.List;

/** A escolha privada que o jogador precisa concluir antes de fazer outra jogada. */
public record PendingChoiceView(
        String prompt,
        List<CardView> options,
        boolean chooseCardFromHand
) implements Serializable {
}
