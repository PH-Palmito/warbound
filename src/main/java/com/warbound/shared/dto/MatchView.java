package com.warbound.shared.dto;

import com.warbound.shared.GameStatus;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record MatchView(
        UUID matchId,
        GameStatus status,
        long version,
        UUID activePlayerId,
        UUID winnerId,
        PlayerView self,
        PlayerView opponent,
        List<CardView> ownHand,
        String message,
        PendingChoiceView pendingChoice
) implements Serializable {
    public MatchView(UUID matchId, GameStatus status, long version, UUID activePlayerId, UUID winnerId,
                     PlayerView self, PlayerView opponent, List<CardView> ownHand, String message) {
        this(matchId, status, version, activePlayerId, winnerId, self, opponent, ownHand, message, null);
    }
}
