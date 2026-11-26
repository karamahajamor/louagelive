package tn.fss.louagelive.server.tcp;

import tn.fss.louagelive.common.Config;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer implements Runnable {
    private final Map<String, Set<ClientHandler>> salons = new ConcurrentHashMap<>();
    private final ExecutorService pool = Executors.newCachedThreadPool();

    @Override
    public void run() {
        try (ServerSocket serveur = new ServerSocket(Config.TCP_PORT)) {
            System.out.println("[TCP] Chat en écoute sur le port " + Config.TCP_PORT);
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serveur.accept();
                pool.submit(new ClientHandler(socket, this));
            }
        } catch (IOException e) {
            System.err.println("[TCP] Arrêt du serveur de chat : " + e.getMessage());
        }
    }

    void rejoindre(String salon, ClientHandler c) {
        salons.computeIfAbsent(salon, k -> ConcurrentHashMap.newKeySet()).add(c);
    }

    void quitter(String salon, ClientHandler c) {
        Set<ClientHandler> s = salons.get(salon);
        if (s != null) s.remove(c);
    }

    void diffuser(String salon, String ligne) {
        for (ClientHandler c : salons.getOrDefault(salon, Collections.<ClientHandler>emptySet())) {
            c.envoyer(ligne);
        }
    }
}