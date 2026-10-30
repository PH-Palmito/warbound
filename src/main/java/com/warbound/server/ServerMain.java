package com.warbound.server;

import com.warbound.server.config.RmiServerConfig;
import com.warbound.server.service.MatchService;
import com.warbound.shared.remote.WarboundRemote;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public final class ServerMain {
    private ServerMain() {
    }

    public static void main(String[] args) throws Exception {
        String advertisedHost = args.length > 0 ? args[0] : "127.0.0.1";
        System.setProperty("java.rmi.server.hostname", advertisedHost);

        Registry registry = LocateRegistry.createRegistry(RmiServerConfig.REGISTRY_PORT);
        WarboundService service = new WarboundService(RmiServerConfig.SERVICE_PORT, new MatchService());
        registry.rebind(WarboundRemote.SERVICE_NAME, service);

        System.out.printf("Servidor Warbound ativo em %s. Registry: %d | Serviço RMI: %d%n",
                advertisedHost, RmiServerConfig.REGISTRY_PORT, RmiServerConfig.SERVICE_PORT);
    }
}
