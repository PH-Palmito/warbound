package com.warbound.server.model;

import com.warbound.shared.CardType;
import com.warbound.shared.Faction;

record CardDefinition(
        String key,
        String name,
        CardType type,
        Faction group,
        int cost,
        int attack,
        int health,
        CardEffect effect,
        String description
) {
    boolean isLowCost() {
        return cost <= 2;
    }
}
