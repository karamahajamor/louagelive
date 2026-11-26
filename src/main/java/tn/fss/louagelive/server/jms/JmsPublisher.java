package tn.fss.louagelive.server.jms;

import tn.fss.louagelive.common.Config;
import tn.fss.louagelive.common.Louage;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class JmsPublisher implements AutoCloseable {
    private final Context ctx;
    private final Connection connection;
    private final Session session;
    private final MessageProducer producer;

    public JmsPublisher() throws NamingException, JMSException {
        ctx = new InitialContext();                                        // lit jndi.properties
        ConnectionFactory cf = (ConnectionFactory) ctx.lookup("ConnectionFactory");
        connection = cf.createConnection();
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        producer = session.createProducer(null);
        producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        System.out.println("[JMS] Connecté au broker via JNDI");
    }

    public synchronized void publierEvenement(Louage l, String type, String texte) {
        try {
            Topic topic = (Topic) ctx.lookup("dynamicTopics/" + Config.topicTrajet(l.getTrajet()));
            MapMessage m = session.createMapMessage();
            m.setString("type", type);
            m.setString("louageId", l.getId());
            m.setInt("placesRestantes", l.placesRestantes());
            m.setString("etat", l.getEtat().name());
            if (texte != null) m.setString("texte", texte);
            producer.send(topic, m);
        } catch (NamingException | JMSException e) {
            System.err.println("[JMS] Échec de publication : " + e.getMessage());
        }
    }

    public synchronized void publierPosition(Louage l) {
        try {
            Topic topic = (Topic) ctx.lookup("TopicPositions");
            MapMessage m = session.createMapMessage();
            m.setString("louageId", l.getId());
            m.setDouble("lat", l.getLat());
            m.setDouble("lon", l.getLon());
            m.setString("etat", l.getEtat().name());
            producer.send(topic, m);
        } catch (NamingException | JMSException e) {
            System.err.println("[JMS] Échec position : " + e.getMessage());
        }
    }

    public synchronized void envoyerConfirmation(String voyageur, String texte) {
        try {
            Queue queue = (Queue) ctx.lookup("dynamicQueues/" + Config.queueVoyageur(voyageur));
            TextMessage m = session.createTextMessage(texte);
            producer.send(queue, m, DeliveryMode.PERSISTENT, Message.DEFAULT_PRIORITY, 0);
        } catch (NamingException | JMSException e) {
            System.err.println("[JMS] Échec confirmation : " + e.getMessage());
        }
    }

    @Override
    public void close() {
        try { connection.close(); } catch (JMSException ignored) { }
    }
}