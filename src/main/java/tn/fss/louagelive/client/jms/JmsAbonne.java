package tn.fss.louagelive.client.jms;

import tn.fss.louagelive.common.Config;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.HashMap;
import java.util.Map;

public class JmsAbonne implements AutoCloseable {
    private final Context ctx;
    private final Connection connection;
    private final Map<String, Session> sessions = new HashMap<>();

    public JmsAbonne() throws NamingException, JMSException {
        ctx = new InitialContext();
        ConnectionFactory cf = (ConnectionFactory) ctx.lookup("ConnectionFactory");
        connection = cf.createConnection();
        connection.start();
    }

    private synchronized void abonner(String cle, String nomJndi, MessageListener listener)
            throws NamingException, JMSException {
        Session ancienne = sessions.remove(cle);
        if (ancienne != null) ancienne.close();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = (Destination) ctx.lookup(nomJndi);
        MessageConsumer consumer = session.createConsumer(destination);
        consumer.setMessageListener(listener);
        sessions.put(cle, session);
    }

    public void abonnerTrajet(String trajet, MessageListener l) throws NamingException, JMSException {
        abonner("trajet", "dynamicTopics/" + Config.topicTrajet(trajet), l);
    }

    public void abonnerPositions(MessageListener l) throws NamingException, JMSException {
        abonner("positions", "TopicPositions", l);
    }

    public void abonnerConfirmations(String voyageur, MessageListener l) throws NamingException, JMSException {
        abonner("confirmations", "dynamicQueues/" + Config.queueVoyageur(voyageur), l);
    }

    public static String texte(Message m) {
        try {
            if (m instanceof TextMessage) return ((TextMessage) m).getText();
            if (m instanceof MapMessage) return ((MapMessage) m).getString("texte");
        } catch (JMSException ignored) { }
        return String.valueOf(m);
    }

    @Override
    public void close() {
        try { connection.close(); } catch (JMSException ignored) { }
    }
}