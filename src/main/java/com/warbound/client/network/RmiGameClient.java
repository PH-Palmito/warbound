package com.warbound.client.network;

import com.warbound.shared.Faction;
import com.warbound.shared.dto.GameCommand;
import com.warbound.shared.dto.GameResult;
import com.warbound.shared.dto.MatchView;
import com.warbound.shared.dto.PlayerSession;
import com.warbound.shared.dto.OpenMatchView;
import com.warbound.shared.remote.WarboundRemote;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.UUID;
import java.util.List;

/**
 * Adaptador de rede do frontend. A interface JavaFX não conhece detalhes de Registry ou Stub RMI.
 */
public final class RmiGameClient {
    private WarboundRemote service;
    private String connectedHost;

    public void connect(String host) throws Exception {
        String normalizedHost = normalizeHost(host);
        if (service != null && normalizedHost.equals(connectedHost)) {
            return;
        }
        Registry registry = LocateRegistry.getRegistry(normalizedHost, 1099);
        service = (WarboundRemote) registry.lookup(WarboundRemote.SERVICE_NAME);
        connectedHost = normalizedHost;
    }

    public PlayerSession createMatch(String playerName, Faction faction) throws Exception {
        return service().createMatch(playerName, faction);
    }

    public PlayerSession joinMatch(UUID matchId, String playerName, Faction faction) throws Exception {
        return service().joinMatch(matchId, playerName, faction);
    }

    public List<OpenMatchView> listOpenMatches() throws Exception {
        return service().listOpenMatches();
    }

    public MatchView getMatch(UUID matchId, UUID playerId, String playerToken) throws Exception {
        return service().getMatch(matchId, playerId, playerToken);
    }

    public GameResult performAction(GameCommand command) throws Exception {
        return service().performAction(command);
    }

    private WarboundRemote service() {
        if (service == null) {
            throw new IllegalStateException("Conecte-se ao servidor antes de continuar.");
        }
        return service;
    }

    private static String normalizeHost(String host) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Informe o endereço do servidor.");
        }
        return host.trim();
    }
}
