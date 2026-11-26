package tn.fss.louagelive.common;

import java.io.Serializable;
import java.util.UUID;

public class Reservation implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id = UUID.randomUUID().toString().substring(0, 8);
    private final String voyageur;
    private final String louageId;
    private final String trajet;
    private final long heure = System.currentTimeMillis();
    private boolean presenceConfirmee;

    public Reservation(String voyageur, String louageId, String trajet) {
        this.voyageur = voyageur;
        this.louageId = louageId;
        this.trajet = trajet;
    }

    public String getId() { return id; }
    public String getVoyageur() { return voyageur; }
    public String getLouageId() { return louageId; }
    public String getTrajet() { return trajet; }
    public long getHeure() { return heure; }
    public boolean isPresenceConfirmee() { return presenceConfirmee; }
    public void setPresenceConfirmee(boolean b) { this.presenceConfirmee = b; }
}