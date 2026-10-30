package com.warbound.server;

import com.warbound.shared.remote.WarboundRemote;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public final class ServerMain {
    private static final int REGISTRY_PORT = 1099;
    private static final int SERVICE_PORT = 5000;

    private ServerMain() {
    }

    public static void main(String[] args) throws Exception {
        String advertisedHost = args.length > 0 ? args[0] : "127.0.0.1";
        System.setProperty("java.rmi.server.hostname", advertisedHost);

        Registry registry = LocateRegistry.createRegistry(REGISTRY_PORT);
        WarboundService service = new WarboundService(SERVICE_PORT);
        registry.rebind(WarboundRemote.SERVICE_NAME, service);

        System.out.printf("Servidor Warbound ativo em %s. Registry: %d | Serviço RMI: %d%n",
                advertisedHost, REGISTRY_PORT, SERVICE_PORT);
    }
}
