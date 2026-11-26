package tn.fss.louagelive.display;

import LouageLive.Depart;
import LouageLive.PanneauPOA;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import tn.fss.louagelive.common.Config;

import java.util.Properties;

public class PanneauJava extends PanneauPOA {

    @Override
    public void afficherDepart(Depart d) {
        System.out.printf("%-12s %-6s %d place(s)  %s%n", d.trajet, d.louageId, d.placesRestantes, d.etat);
    }

    @Override
    public void afficherMessage(String texte) {
        System.out.println(">>> " + texte);
    }

    @Override
    public String ping() { return "Panneau Java OK"; }

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put("org.omg.CORBA.ORBInitialHost", Config.HOST);
        props.put("org.omg.CORBA.ORBInitialPort", Config.CORBA_PORT);
        ORB orb = ORB.init(args, props);

        POA racine = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
        racine.the_POAManager().activate();
        org.omg.CORBA.Object ref = racine.servant_to_reference(new PanneauJava());

        NamingContextExt nc = NamingContextExtHelper.narrow(orb.resolve_initial_references("NameService"));
        nc.rebind(nc.to_name(Config.PANNEAU_NOM), ref);
        System.out.println("Panneau Java enregistré sous '" + Config.PANNEAU_NOM + "'");
        orb.run();
    }
}
