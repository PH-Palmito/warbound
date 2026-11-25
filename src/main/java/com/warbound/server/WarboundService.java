package com.warbound.server;

import com.warbound.server.service.MatchService;
import com.warbound.shared.Faction;
import com.warbound.shared.dto.GameCommand;
import com.warbound.shared.dto.GameResult;
import com.warbound.shared.dto.MatchView;
import com.warbound.shared.dto.PlayerSession;
import com.warbound.shared.dto.OpenMatchView;
import com.warbound.shared.remote.WarboundRemote;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;
import java.util.List;

/** Adaptador RMI: recebe a chamada remota e delega a regra de aplicação ao MatchService. */
public final class WarboundService extends UnicastRemoteObject implements WarboundRemote {
    private final MatchService matchService;

    public WarboundService(int servicePort, MatchService matchService) throws RemoteException {
        super(servicePort);
        this.matchService = matchService;
    }

    @Override
    public PlayerSession createMatch(String playerName, Faction faction) {
        return matchService.createMatch(playerName, faction);
    }

    @Override
    public PlayerSession joinMatch(UUID matchId, String playerName, Faction faction) {
        return matchService.joinMatch(matchId, playerName, faction);
    }

    @Override
    public List<OpenMatchView> listOpenMatches() {
        return matchService.listOpenMatches();
    }

    @Override
    public MatchView getMatch(UUID matchId, UUID playerId, String playerToken) {
        return matchService.getMatch(matchId, playerId, playerToken);
    }

    @Override
    public GameResult performAction(GameCommand command) {
        return matchService.performAction(command);
    }
}
