package tn.fss.louagelive.server;

import java.util.Random;

public class SimulateurGuichet implements Runnable {
    private final StationServiceImpl station;
    private final Random random = new Random();
    private int compteur = 1;

    public SimulateurGuichet(StationServiceImpl station) { this.station = station; }

    @Override
    public void run() {
        String[] trajets = {"SFAX-TUNIS", "SFAX-SOUSSE", "SFAX-GABES"};
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(6000 + random.nextInt(6000));
                station.ajouterPassagerSurPlace(trajets[random.nextInt(trajets.length)], "Guichet-" + compteur++);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                System.err.println("[Guichet] " + e.getMessage());
            }
        }
    }
}