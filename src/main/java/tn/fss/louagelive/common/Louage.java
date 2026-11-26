package tn.fss.louagelive.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Louage implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int CAPACITE = 8;

    private final String id;
    private final String matricule;
    private final String chauffeur;
    private final String trajet;
    private final List<String> passagers = new ArrayList<>();
    private EtatLouage etat = EtatLouage.EN_ATTENTE;
    private double lat;
    private double lon;
    private long debutRemplissage;
    private long dernierSignal;
    private boolean signalPerdu;

    public Louage(String id, String matricule, String chauffeur, String trajet) {
        this.id = id;
        this.matricule = matricule;
        this.chauffeur = chauffeur;
        this.trajet = trajet;
        reinitialiser();
    }

    public void demarrerRemplissage() {
        etat = EtatLouage.EN_REMPLISSAGE;
        debutRemplissage = System.currentTimeMillis();
    }

    public void reinitialiser() {
        passagers.clear();
        etat = EtatLouage.EN_ATTENTE;
        signalPerdu = false;
        Ville depart = Ville.depart(trajet);
        lat = depart.lat;
        lon = depart.lon;
    }

    public int placesRestantes() { return CAPACITE - passagers.size(); }
    public boolean estComplet()  { return passagers.size() >= CAPACITE; }

    public String getId() { return id; }
    public String getMatricule() { return matricule; }
    public String getChauffeur() { return chauffeur; }
    public String getTrajet() { return trajet; }
    public List<String> getPassagers() { return passagers; }
    public EtatLouage getEtat() { return etat; }
    public void setEtat(EtatLouage etat) { this.etat = etat; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public void setPosition(double lat, double lon) { this.lat = lat; this.lon = lon; }
    public long getDebutRemplissage() { return debutRemplissage; }
    public long getDernierSignal() { return dernierSignal; }
    public void setDernierSignal(long t) { this.dernierSignal = t; }
    public boolean isSignalPerdu() { return signalPerdu; }
    public void setSignalPerdu(boolean b) { this.signalPerdu = b; }
}