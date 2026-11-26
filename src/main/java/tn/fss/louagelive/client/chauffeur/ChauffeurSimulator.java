package tn.fss.louagelive.client.chauffeur;
import tn.fss.louagelive.client.tcp.ChatClient;
import tn.fss.louagelive.common.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.util.Locale;
import java.util.Scanner;
public class ChauffeurSimulator {


        public static void main(String[] args) throws Exception {
            if (args.length < 1) {
                System.out.println("Usage : ChauffeurSimulator <louageId>   (ex. TUN-1)");
                return;
            }
            String id = args[0];
            StationService station = (StationService) LocateRegistry
                    .getRegistry(Config.HOST, Config.RMI_PORT).lookup(Config.RMI_NAME);
            Louage louage = station.getLouage(id);
            if (louage == null) { System.out.println("Louage inconnu : " + id); return; }

            Thread gps = new Thread(() -> envoyerPositions(station, id), "gps-udp");
            gps.setDaemon(true);
            gps.start();

            String nom = "Chauffeur " + louage.getChauffeur();
            try (ChatClient chat = new ChatClient(); Scanner clavier = new Scanner(System.in, "UTF-8")) {
                chat.connecter(Config.HOST, Config.TCP_PORT, id, nom, System.out::println);
                System.out.println("Tapez un message pour vos passagers (quit pour sortir) :");
                while (clavier.hasNextLine()) {
                    String ligne = clavier.nextLine();
                    if ("quit".equalsIgnoreCase(ligne)) break;
                    chat.envoyer(ligne);
                }
            }
        }

        private static void envoyerPositions(StationService station, String id) {
            double progression = 0;
            try (DatagramSocket socket = new DatagramSocket()) {
                InetAddress adresse = InetAddress.getByName(Config.HOST);
                while (true) {
                    Louage l = station.getLouage(id);
                    Ville dep = Ville.depart(l.getTrajet());
                    Ville arr = Ville.arrivee(l.getTrajet());
                    if (l.getEtat() == EtatLouage.EN_ROUTE) progression = Math.min(1.0, progression + 0.05);
                    else if (l.getEtat() != EtatLouage.ARRIVE) progression = 0;
                    double lat = dep.lat + (arr.lat - dep.lat) * progression;
                    double lon = dep.lon + (arr.lon - dep.lon) * progression;
                    String statut = progression >= 1.0 ? "ARRIVE" : l.getEtat().name();
                    String msg = String.format(Locale.US, "%s;%.5f;%.5f;%s", id, lat, lon, statut);
                    byte[] data = msg.getBytes(StandardCharsets.UTF_8);
                    socket.send(new DatagramPacket(data, data.length, adresse, Config.UDP_PORT));
                    Thread.sleep(3000);
                }
            } catch (Exception e) {
                System.err.println("[GPS] Arrêt : " + e.getMessage());
            }
        }
    }

