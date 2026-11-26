package tn.fss.louagelive.server.udp;

import tn.fss.louagelive.common.Config;
import tn.fss.louagelive.server.StationServiceImpl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

public class PositionReceiver implements Runnable {
    private final StationServiceImpl service;

    public PositionReceiver(StationServiceImpl service) { this.service = service; }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(Config.UDP_PORT)) {
            System.out.println("[UDP] Réception des positions sur le port " + Config.UDP_PORT);
            byte[] tampon = new byte[256];
            while (!Thread.currentThread().isInterrupted()) {
                DatagramPacket paquet = new DatagramPacket(tampon, tampon.length);
                socket.receive(paquet);
                String msg = new String(paquet.getData(), 0, paquet.getLength(), StandardCharsets.UTF_8);
                String[] p = msg.split(";");
                if (p.length < 3) continue;
                try {
                    service.majPosition(p[0], Double.parseDouble(p[1]), Double.parseDouble(p[2]),
                            p.length > 3 ? p[3] : "");
                } catch (NumberFormatException e) {
                    System.err.println("[UDP] Coordonnées invalides : " + msg);
                }
            }
        } catch (IOException e) {
            System.err.println("[UDP] Arrêt du récepteur : " + e.getMessage());
        }
    }
}