package tn.fss.louagelive.server;

import tn.fss.louagelive.common.Config;
import tn.fss.louagelive.server.corba.PanneauNotifier;
import tn.fss.louagelive.server.jms.JmsPublisher;
import tn.fss.louagelive.server.tcp.ChatServer;
import tn.fss.louagelive.server.udp.PositionReceiver;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class StationServer {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.rmi.server.hostname", Config.HOST);

        JmsPublisher jms = new JmsPublisher();
        PanneauNotifier panneau = new PanneauNotifier(args);
        StationServiceImpl service = new StationServiceImpl(jms, panneau);

        Registry registry = LocateRegistry.createRegistry(Config.RMI_PORT);
        registry.rebind(Config.RMI_NAME, service);
        System.out.println("[RMI] StationService publié sur le port " + Config.RMI_PORT);

        new Thread(new PositionReceiver(service), "udp-positions").start();
        new Thread(new ChatServer(), "tcp-chat").start();
        new Thread(new SimulateurGuichet(service), "guichet").start();
        service.publierEtatInitial();
        System.out.println("Station LouageLive prête.");
    }
}