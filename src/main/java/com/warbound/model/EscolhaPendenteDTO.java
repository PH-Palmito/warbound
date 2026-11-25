package com.warbound.model;

import java.util.List;

/** Escolha privada que o servidor exige antes de permitir outra jogada. */
public record EscolhaPendenteDTO(String instrucao, List<Carta> opcoes, boolean descartarDaMao) {
}
