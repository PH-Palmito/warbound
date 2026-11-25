package com.warbound.shared.dto;

import com.warbound.shared.ActionType;

import java.io.Serializable;
import java.util.UUID;

public record GameCommand(
        UUID matchId,
        UUID playerId,
        String playerToken,
        ActionType action,
        String sourceCardId,
        String targetUnitId,
        long expectedVersion,
        String choiceCardId
) implements Serializable {
    public GameCommand(UUID matchId, UUID playerId, String playerToken, ActionType action,
                       String sourceCardId, String targetUnitId, long expectedVersion) {
        this(matchId, playerId, playerToken, action, sourceCardId, targetUnitId, expectedVersion, null);
    }
}
