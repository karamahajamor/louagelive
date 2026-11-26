package tn.fss.louagelive.server.tcp;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer serveur;
    private PrintWriter out;
    private String salon;
    private String nom;

    ClientHandler(Socket socket, ChatServer serveur) {
        this.socket = socket;
        this.serveur = serveur;
    }

    @Override
    public void run() {
        try (Socket s = socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))) {
            out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
            String premiere = in.readLine();                     // JOIN;<louageId>;<nom>
            if (premiere == null || !premiere.startsWith("JOIN;")) return;
            String[] p = premiere.split(";", 3);
            if (p.length < 3) return;
            salon = p[1];
            nom = p[2];
            serveur.rejoindre(salon, this);
            serveur.diffuser(salon, "*** " + nom + " a rejoint le louage " + salon);
            String ligne;
            while ((ligne = in.readLine()) != null) {
                if (ligne.startsWith("MSG;")) serveur.diffuser(salon, nom + " : " + ligne.substring(4));
            }
        } catch (IOException e) {
            // client déconnecté brutalement : on passe au nettoyage
        } finally {
            if (salon != null) {
                serveur.quitter(salon, this);
                serveur.diffuser(salon, "*** " + nom + " a quitté le chat");
            }
        }
    }

    // synchronized : plusieurs threads peuvent diffuser vers ce client en même temps
    synchronized void envoyer(String ligne) {
        if (out != null) out.println(ligne);
    }
}