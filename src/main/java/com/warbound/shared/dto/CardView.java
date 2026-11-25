package com.warbound.shared.dto;

import com.warbound.shared.CardType;
import com.warbound.shared.Faction;

import java.io.Serializable;

public record CardView(
        String instanceId,
        String name,
        int cost,
        int attack,
        int health,
        CardType type,
        Faction group,
        String description
) implements Serializable {
    public CardView(String instanceId, String name, int cost, int attack, int health) {
        this(instanceId, name, cost, attack, health, CardType.CHARACTER, null, "");
    }
}
