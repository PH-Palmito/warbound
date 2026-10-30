package com.warbound.shared.dto;

import java.io.Serializable;

public record CardView(
        String instanceId,
        String name,
        int cost,
        int attack,
        int health
) implements Serializable {
}
