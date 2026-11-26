package tn.fss.louagelive.client.tcp;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class ChatClient implements AutoCloseable {

        private Socket socket;
        private PrintWriter out;

        public void connecter(String host, int port, String louageId, String nom, Consumer<String> recepteur)
                throws IOException {
            socket = new Socket(host, port);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out.println("JOIN;" + louageId + ";" + nom);
            Thread lecteur = new Thread(() -> {
                try {
                    String ligne;
                    while ((ligne = in.readLine()) != null) recepteur.accept(ligne);
                } catch (IOException e) {
                    recepteur.accept("*** Connexion au chat fermée");
                }
            }, "chat-lecteur");
            lecteur.setDaemon(true);
            lecteur.start();
        }

        public void envoyer(String texte) {
            if (out != null && !texte.trim().isEmpty()) out.println("MSG;" + texte);
        }

        @Override
        public void close() {
            try { if (socket != null) socket.close(); } catch (IOException ignored) { }
        }
    }

