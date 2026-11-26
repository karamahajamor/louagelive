package tn.fss.louagelive.server;

import tn.fss.louagelive.common.*;
import tn.fss.louagelive.server.corba.PanneauNotifier;
import tn.fss.louagelive.server.jms.JmsPublisher;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StationServiceImpl extends UnicastRemoteObject implements StationService {

    private final Map<String, List<Louage>> louagesParTrajet = new LinkedHashMap<>();
    private final Map<String, Louage> louagesParId = new HashMap<>();
    private final Map<String, Reservation> reservations = new HashMap<>();
    private final JmsPublisher jms;
    private final PanneauNotifier panneau;
    private final ScheduledExecutorService planificateur = Executors.newScheduledThreadPool(2);

    public StationServiceImpl(JmsPublisher jms, PanneauNotifier panneau) throws RemoteException {
        super();
        this.jms = jms;
        this.panneau = panneau;
        String[] chauffeurs = {"Ali", "Mohamed", "Sami", "Hedi", "Karim", "Nizar", "Walid", "Fathi", "Anis"};
        int n = 0;
        for (String trajet : new String[]{"SFAX-TUNIS", "SFAX-SOUSSE", "SFAX-GABES"}) {
            List<Louage> file = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                String id = trajet.substring(5, 8) + "-" + i;
                Louage l = new Louage(id, (180 + n) + " TU " + (1000 + 37 * n), chauffeurs[n], trajet);
                n++;
                file.add(l);
                louagesParId.put(id, l);
            }
            file.get(0).demarrerRemplissage();
            louagesParTrajet.put(trajet, file);
        }
        planificateur.scheduleAtFixedRate(this::verifierSignaux, 5, 5, TimeUnit.SECONDS);
    }

    // ---------- Méthodes distantes (RMI) ----------

    @Override
    public synchronized List<String> getTrajets() {
        return new ArrayList<>(louagesParTrajet.keySet());
    }

    @Override
    public synchronized List<Louage> getLouages(String trajet) {
        return new ArrayList<>(louagesParTrajet.getOrDefault(trajet, Collections.<Louage>emptyList()));
    }

    @Override
    public synchronized Louage getLouage(String louageId) {
        return louagesParId.get(louageId);
    }

    @Override
    public synchronized Reservation reserverPlace(String voyageur, String trajet) throws ReservationException {
        Louage l = enRemplissage(trajet);
        if (l == null) throw new ReservationException("Aucun louage en remplissage pour " + trajet);
        if (l.getPassagers().contains(voyageur))
            throw new ReservationException("Vous avez déjà une place dans " + l.getId());
        l.getPassagers().add(voyageur);
        Reservation r = new Reservation(voyageur, l.getId(), trajet);
        reservations.put(r.getId(), r);
        jms.envoyerConfirmation(voyageur, "Place réservée dans " + l.getId() + " (" + l.getMatricule()
                + "). Confirmez votre présence sous " + Config.DELAI_CONFIRMATION_S + " s.");
        planificateur.schedule(() -> expirer(r.getId()), Config.DELAI_CONFIRMATION_S, TimeUnit.SECONDS);
        apresChangement(l);
        return r;
    }

    @Override
    public synchronized void confirmerPresence(String reservationId) throws ReservationException {
        Reservation r = trouver(reservationId);
        r.setPresenceConfirmee(true);
        jms.envoyerConfirmation(r.getVoyageur(), "Présence confirmée. Bon voyage !");
    }

    @Override
    public synchronized void annulerReservation(String reservationId) throws ReservationException {
        Reservation r = trouver(reservationId);
        Louage l = louagesParId.get(r.getLouageId());
        if (l.getEtat() != EtatLouage.EN_REMPLISSAGE)
            throw new ReservationException("Trop tard : le louage est " + l.getEtat());
        l.getPassagers().remove(r.getVoyageur());
        reservations.remove(reservationId);
        jms.envoyerConfirmation(r.getVoyageur(), "Réservation " + reservationId + " annulée.");
        apresChangement(l);
    }

    @Override
    public synchronized int estimerAttenteMinutes(String trajet) {
        Louage l = enRemplissage(trajet);
        if (l == null) return -1;
        int heure = LocalTime.now().getHour();
        boolean pointe = (heure >= 6 && heure < 9) || (heure >= 16 && heure < 19);
        int minutesParVoyageur = pointe ? 2 : 5;
        return l.placesRestantes() * minutesParVoyageur;
    }

    // ---------- Méthodes locales (UDP, guichet, démarrage) ----------

    public synchronized void publierEtatInitial() {
        for (Louage l : louagesParId.values()) panneau.afficher(l);
    }

    public synchronized void ajouterPassagerSurPlace(String trajet, String nom) {
        Louage l = enRemplissage(trajet);
        if (l == null) return;
        l.getPassagers().add(nom);
        apresChangement(l);
    }

    public synchronized void majPosition(String id, double lat, double lon, String statut) {
        Louage l = louagesParId.get(id);
        if (l == null) return;
        l.setPosition(lat, lon);
        l.setDernierSignal(System.currentTimeMillis());
        if (l.isSignalPerdu()) {
            l.setSignalPerdu(false);
            jms.publierEvenement(l, "SIGNAL_RETROUVE", "Signal GPS retrouvé pour " + id);
        }
        if ("ARRIVE".equals(statut) && l.getEtat() == EtatLouage.EN_ROUTE) {
            l.setEtat(EtatLouage.ARRIVE);
            jms.publierEvenement(l, "ARRIVEE", id + " est arrivé à " + Ville.arrivee(l.getTrajet()));
            panneau.afficher(l);
            planificateur.schedule(() -> recycler(id), 20, TimeUnit.SECONDS);
        }
        jms.publierPosition(l);
    }

    // ---------- Logique interne ----------

    private void apresChangement(Louage l) {
        if (l.estComplet() && l.getEtat() == EtatLouage.EN_REMPLISSAGE) {
            l.setEtat(EtatLouage.COMPLET);
            jms.publierEvenement(l, "DEPART_IMMINENT",
                    l.getId() + " est complet : départ dans " + Config.DELAI_DEPART_S + " s !");
            panneau.message("Louage " + l.getId() + " complet - départ imminent");
            planificateur.schedule(() -> partir(l.getId()), Config.DELAI_DEPART_S, TimeUnit.SECONDS);
        }
        jms.publierEvenement(l, "ETAT", null);
        panneau.afficher(l);
    }

    private synchronized void partir(String id) {
        Louage l = louagesParId.get(id);
        l.setEtat(EtatLouage.EN_ROUTE);
        l.setDernierSignal(System.currentTimeMillis());
        jms.publierEvenement(l, "DEPART", id + " a quitté la station de Sfax.");
        panneau.afficher(l);
        for (Louage suivant : louagesParTrajet.get(l.getTrajet())) {
            if (suivant.getEtat() == EtatLouage.EN_ATTENTE) {
                suivant.demarrerRemplissage();
                apresChangement(suivant);
                break;
            }
        }
    }

    private synchronized void expirer(String reservationId) {
        Reservation r = reservations.get(reservationId);
        if (r == null || r.isPresenceConfirmee()) return;
        Louage l = louagesParId.get(r.getLouageId());
        if (l.getEtat() != EtatLouage.EN_REMPLISSAGE) return;
        l.getPassagers().remove(r.getVoyageur());
        reservations.remove(reservationId);
        jms.envoyerConfirmation(r.getVoyageur(),
                "Réservation " + reservationId + " expirée : présence non confirmée.");
        apresChangement(l);
    }

    private synchronized void recycler(String id) {
        Louage l = louagesParId.get(id);
        l.reinitialiser();
        reservations.values().removeIf(r -> r.getLouageId().equals(id));
        if (enRemplissage(l.getTrajet()) == null) l.demarrerRemplissage();
        apresChangement(l);
    }

    private synchronized void verifierSignaux() {
        try {
            long maintenant = System.currentTimeMillis();
            for (Louage l : louagesParId.values()) {
                if (l.getEtat() == EtatLouage.EN_ROUTE && !l.isSignalPerdu()
                        && maintenant - l.getDernierSignal() > 10_000) {
                    l.setSignalPerdu(true);
                    jms.publierEvenement(l, "SIGNAL_PERDU", "Signal GPS perdu pour " + l.getId() + " depuis 10 s");
                }
            }
        } catch (RuntimeException e) {
            System.err.println("[Signaux] " + e.getMessage());
        }
    }

    private Louage enRemplissage(String trajet) {
        for (Louage l : louagesParTrajet.getOrDefault(trajet, Collections.<Louage>emptyList()))
            if (l.getEtat() == EtatLouage.EN_REMPLISSAGE) return l;
        return null;
    }

    private Reservation trouver(String id) throws ReservationException {
        Reservation r = reservations.get(id);
        if (r == null) throw new ReservationException("Réservation introuvable : " + id);
        return r;
    }
}