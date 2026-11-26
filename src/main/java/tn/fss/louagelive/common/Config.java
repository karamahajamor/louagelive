package tn.fss.louagelive.common;

public final class Config {
    public static final String HOST = System.getProperty("louage.host", "localhost");
    public static final int RMI_PORT = 1099;
    public static final int UDP_PORT = 5000;
    public static final int TCP_PORT = 6000;
    public static final String RMI_NAME = "StationService";

    public static final String CORBA_PORT = "1050";
    public static final String PANNEAU_NOM = "PanneauStation";
    public static final String IOR_PANNEAU = "display-python/panneau.ior";

    public static final int DELAI_CONFIRMATION_S = 60;
    public static final int DELAI_DEPART_S = 30;

    public static String topicTrajet(String trajet) {
        return "trajet." + trajet.toLowerCase();
    }

    public static String queueVoyageur(String nom) {
        return "confirmation." + nom.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }

    private Config() { }
}