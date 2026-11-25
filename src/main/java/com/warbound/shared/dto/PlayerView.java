package com.warbound.shared.dto;

import com.warbound.shared.Faction;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record PlayerView(
        UUID playerId,
        String name,
        Faction faction,
        int commanderHealth,
        int mana,
        int maxMana,
        int deckSize,
        int handSize,
        List<UnitView> board,
        List<UnitView> reserve,
        int discardSize,
        int livesLost,
        String activeGroupBuff
) implements Serializable {
    public PlayerView(UUID playerId, String name, Faction faction, int commanderHealth, int mana,
                      int maxMana, int deckSize, int handSize, List<UnitView> board) {
        this(playerId, name, faction, commanderHealth, mana, maxMana, deckSize, handSize,
                board, List.of(), 0, 0, null);
    }
}
