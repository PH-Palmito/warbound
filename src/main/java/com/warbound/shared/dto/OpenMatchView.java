package com.warbound.shared.dto;

import java.io.Serializable;
import java.util.UUID;

/** Resumo público de uma partida que ainda aceita o segundo jogador. */
public record OpenMatchView(UUID matchId, String ownerName) implements Serializable {
}
