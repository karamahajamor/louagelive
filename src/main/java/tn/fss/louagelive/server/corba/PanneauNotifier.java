package tn.fss.louagelive.server.corba;

import tn.fss.louagelive.common.Louage;
import LouageLive.Depart;
import LouageLive.Panneau;
import LouageLive.PanneauHelper;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import tn.fss.louagelive.common.Config;
import tn.fss.louagelive.common.Louage;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class PanneauNotifier {
    private final ORB orb;
    private Panneau panneau;
    private long derniereTentative;

    public PanneauNotifier(String[] args) {
        Properties props = new Properties();
        props.put("org.omg.CORBA.ORBInitialHost", Config.HOST);
        props.put("org.omg.CORBA.ORBInitialPort", Config.CORBA_PORT);
        orb = ORB.init(args, props);
        connecter();
    }

    private void connecter() {
        derniereTentative = System.currentTimeMillis();
        try {
            NamingContextExt nc = NamingContextExtHelper.narrow(orb.resolve_initial_references("NameService"));
            panneau = PanneauHelper.narrow(nc.resolve_str(Config.PANNEAU_NOM));
        } catch (Exception e) {
            try {
                String ior = new String(Files.readAllBytes(Paths.get(Config.IOR_PANNEAU)), StandardCharsets.UTF_8).trim();
                panneau = PanneauHelper.narrow(orb.string_to_object(ior));
            } catch (Exception e2) {
                panneau = null;
            }
        }
        if (panneau != null) {
            try {
                System.out.println("[CORBA] " + panneau.ping());
            } catch (org.omg.CORBA.SystemException e) {
                panneau = null;
            }
        }
        if (panneau == null) System.out.println("[CORBA] Panneau indisponible, nouvelle tentative dans 10 s");
    }

    private boolean pret() {
        if (panneau == null && System.currentTimeMillis() - derniereTentative > 10_000) connecter();
        return panneau != null;
    }

    public synchronized void afficher(Louage l) {
        if (!pret()) return;
        try {
            panneau.afficherDepart(new Depart(l.getId(), l.getTrajet(), l.placesRestantes(), l.getEtat().name()));
        } catch (org.omg.CORBA.SystemException e) {
            System.err.println("[CORBA] Panneau perdu : " + e);
            panneau = null;
        }
    }

    public synchronized void message(String texte) {
        if (!pret()) return;
        try {
            panneau.afficherMessage(texte);
        } catch (org.omg.CORBA.SystemException e) {
            panneau = null;
        }
    }
}