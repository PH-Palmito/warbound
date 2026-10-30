package com.warbound.shared.dto;

import java.io.Serializable;

public record UnitView(
        String instanceId,
        String name,
        int attack,
        int health,
        boolean readyToAttack
) implements Serializable {
}
