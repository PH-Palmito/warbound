package com.warbound.shared.dto;

import java.io.Serializable;
import java.util.UUID;

public record PlayerSession(
        UUID matchId,
        UUID playerId,
        String playerToken,
        int playerOrder
) implements Serializable {
}
