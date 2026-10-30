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
        List<UnitView> board
) implements Serializable {
}
