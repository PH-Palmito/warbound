package com.warbound.server.service;

import com.warbound.server.model.MatchState;
import com.warbound.shared.Faction;
import com.warbound.shared.dto.GameCommand;
import com.warbound.shared.dto.GameResult;
import com.warbound.shared.dto.MatchView;
import com.warbound.shared.dto.PlayerSession;
import com.warbound.shared.dto.OpenMatchView;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Camada de aplicação: gerencia o ciclo de vida das partidas sem depender do RMI.
 */
public final class MatchService {
    private final Map<UUID, MatchState> matches = new ConcurrentHashMap<>();

    public PlayerSession createMatch(String playerName, Faction faction) {
        MatchState match = new MatchState(playerName, faction);
        matches.put(match.id(), match);
        return match.ownerSession();
    }

    public PlayerSession joinMatch(UUID matchId, String playerName, Faction faction) {
        return requireMatch(matchId).join(playerName, faction);
    }

    public List<OpenMatchView> listOpenMatches() {
        return matches.values().stream()
                .filter(MatchState::isWaitingForPlayers)
                .map(match -> new OpenMatchView(match.id(), match.ownerName()))
                .sorted(Comparator.comparing(OpenMatchView::ownerName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(view -> view.matchId().toString()))
                .toList();
    }

    public MatchView getMatch(UUID matchId, UUID playerId, String playerToken) {
        return requireMatch(matchId).viewFor(playerId, playerToken);
    }

    public GameResult performAction(GameCommand command) {
        if (command == null || command.matchId() == null) {
            return new GameResult(false, "Comando de jogada inválido.", null);
        }
        return requireMatch(command.matchId()).perform(command);
    }

    private MatchState requireMatch(UUID matchId) {
        MatchState match = matches.get(matchId);
        if (match == null) {
            throw new IllegalArgumentException("Partida não encontrada.");
        }
        return match;
    }
}
