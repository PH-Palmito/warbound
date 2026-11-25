package com.warbound.shared.remote;

import com.warbound.shared.Faction;
import com.warbound.shared.dto.GameCommand;
import com.warbound.shared.dto.GameResult;
import com.warbound.shared.dto.MatchView;
import com.warbound.shared.dto.PlayerSession;
import com.warbound.shared.dto.OpenMatchView;

import java.util.List;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

public interface WarboundRemote extends Remote {
    String SERVICE_NAME = "WarboundService";

    PlayerSession createMatch(String playerName, Faction faction) throws RemoteException;

    PlayerSession joinMatch(UUID matchId, String playerName, Faction faction) throws RemoteException;

    List<OpenMatchView> listOpenMatches() throws RemoteException;

    MatchView getMatch(UUID matchId, UUID playerId, String playerToken) throws RemoteException;

    GameResult performAction(GameCommand command) throws RemoteException;
}
