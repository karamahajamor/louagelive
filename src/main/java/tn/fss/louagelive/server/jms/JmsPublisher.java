package tn.fss.louagelive.server.jms;

import tn.fss.louagelive.common.Louage;

public class JmsPublisher {                       // version provisoire
    public JmsPublisher() { }
    public synchronized void publierEvenement(Louage l, String type, String texte) { }
    public synchronized void publierPosition(Louage l) { }
    public synchronized void envoyerConfirmation(String voyageur, String texte) { }
}