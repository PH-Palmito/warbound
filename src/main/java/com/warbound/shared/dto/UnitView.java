package com.warbound.shared.dto;

import java.io.Serializable;

public record UnitView(
        String instanceId,
        String name,
        int attack,
        int health,
        boolean readyToAttack,
        int maxHealth,
        int slot,
        String group,
        boolean protectedFromAttacks,
        int chargeTurnsRemaining,
        boolean furyActive
) implements Serializable {
    public UnitView(String instanceId, String name, int attack, int health, boolean readyToAttack) {
        this(instanceId, name, attack, health, readyToAttack, health, -1, "", false, 0, false);
    }
}
