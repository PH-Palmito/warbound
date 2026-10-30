package com.warbound.server;

import com.warbound.server.model.MatchState;
import com.warbound.shared.Faction;
import com.warbound.shared.dto.GameCommand;
import com.warbound.shared.dto.GameResult;
import com.warbound.shared.dto.MatchView;
import com.warbound.shared.dto.PlayerSession;
import com.warbound.shared.remote.WarboundRemote;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WarboundService extends UnicastRemoteObject implements WarboundRemote {
    private final Map<UUID, MatchState> matches = new ConcurrentHashMap<>();

    public WarboundService(int servicePort) throws RemoteException {
        super(servicePort);
    }

    @Override
    public PlayerSession createMatch(String playerName, Faction faction) {
        MatchState match = new MatchState(playerName, faction);
        matches.put(match.id(), match);
        return match.ownerSession();
    }

    @Override
    public PlayerSession joinMatch(UUID matchId, String playerName, Faction faction) {
        return requireMatch(matchId).join(playerName, faction);
    }

    @Override
    public MatchView getMatch(UUID matchId, UUID playerId, String playerToken) {
        return requireMatch(matchId).viewFor(playerId, playerToken);
    }

    @Override
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
