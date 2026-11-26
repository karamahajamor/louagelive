package tn.fss.louagelive.client.voyageur;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.fss.louagelive.client.jms.JmsAbonne;
import tn.fss.louagelive.client.tcp.ChatClient;
import tn.fss.louagelive.common.*;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class VoyageurApp extends Application {
    private StationService station;
    private JmsAbonne jms;
    private ChatClient chat;
    private Reservation reservation;
    private String nom;

    private final ExecutorService arrierePlan = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "appels-rmi");
        t.setDaemon(true);
        return t;
    });

    private final TextField nomField = new TextField();
    private final ComboBox<String> trajetBox = new ComboBox<>();
    private final VBox louagesBox = new VBox(8);
    private final Label attenteLabel = new Label("Choisissez un trajet");
    private final ListView<String> notifications = new ListView<>();
    private final TextArea chatArea = new TextArea();
    private final TextField chatField = new TextField();
    private final CarteCanvas carte = new CarteCanvas(320, 260);
    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        try {
            station = (StationService) LocateRegistry.getRegistry(Config.HOST, Config.RMI_PORT).lookup(Config.RMI_NAME);
            jms = new JmsAbonne();
            trajetBox.getItems().setAll(station.getTrajets());
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Serveur injoignable : " + e.getMessage()).showAndWait();
            Platform.exit();
            return;
        }
        trajetBox.getSelectionModel().selectFirst();
        nomField.setPromptText("Votre nom");

        Button suivreBtn = new Button("Suivre ce trajet");
        suivreBtn.setOnAction(e -> suivreTrajet());
        HBox haut = new HBox(10, new Label("Nom :"), nomField, new Label("Trajet :"), trajetBox, suivreBtn);
        haut.setAlignment(Pos.CENTER_LEFT);
        haut.setPadding(new Insets(10));

        Button reserverBtn = new Button("Réserver une place");
        Button presentBtn = new Button("Je suis à la station");
        Button annulerBtn = new Button("Annuler");
        reserverBtn.setOnAction(e -> reserver());
        presentBtn.setOnAction(e -> confirmerPresence());
        annulerBtn.setOnAction(e -> annuler());
        attenteLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        VBox gauche = new VBox(10, new Label("Louages en station"), louagesBox, attenteLabel,
                new HBox(8, reserverBtn, presentBtn, annulerBtn));
        gauche.setPadding(new Insets(10));

        notifications.setPrefHeight(220);
        VBox droite = new VBox(8, new Label("Carte"), carte, new Label("Notifications"), notifications);
        droite.setPadding(new Insets(10));

        chatArea.setEditable(false);
        chatArea.setPrefRowCount(6);
        chatField.setPromptText("Message au chauffeur et aux passagers... (Entrée pour envoyer)");
        chatField.setOnAction(e -> {
            if (chat != null) { chat.envoyer(chatField.getText()); chatField.clear(); }
        });
        VBox bas = new VBox(6, new Label("Chat du louage"), chatArea, chatField);
        bas.setPadding(new Insets(10));

        BorderPane racine = new BorderPane();
        racine.setTop(haut);
        racine.setCenter(gauche);
        racine.setRight(droite);
        racine.setBottom(bas);
        stage.setScene(new Scene(racine, 900, 780));
        stage.setTitle("LouageLive - Voyageur");
        stage.show();
    }

    // ---------- Abonnements JMS ----------

    private void suivreTrajet() {
        nom = nomField.getText().trim();
        if (nom.isEmpty()) { notifier("Entrez d'abord votre nom"); return; }
        String trajet = trajetBox.getValue();
        try {
            jms.abonnerTrajet(trajet, m -> Platform.runLater(() -> surEvenementTrajet(m)));
            jms.abonnerConfirmations(nom, m -> Platform.runLater(() -> notifier(JmsAbonne.texte(m))));
            jms.abonnerPositions(m -> Platform.runLater(() -> surPosition(m)));
            notifier("Abonné au trajet " + trajet);
            rafraichir();
        } catch (Exception e) {
            notifier("Erreur JMS : " + e.getMessage());
        }
    }

    private void surEvenementTrajet(Message m) {
        try {
            MapMessage mm = (MapMessage) m;
            if (!"ETAT".equals(mm.getString("type"))) notifier(mm.getString("texte"));
        } catch (JMSException e) {
            notifier("Message illisible");
        }
        rafraichir();
    }

    private void surPosition(Message m) {
        try {
            MapMessage mm = (MapMessage) m;
            carte.majPosition(mm.getString("louageId"), mm.getDouble("lat"), mm.getDouble("lon"), mm.getString("etat"));
        } catch (JMSException ignored) { }
    }

    // ---------- Actions RMI ----------

    private void reserver() {
        if (nom == null) { notifier("Cliquez d'abord sur « Suivre ce trajet »"); return; }
        String trajet = trajetBox.getValue();
        async(() -> station.reserverPlace(nom, trajet), r -> {
            reservation = r;
            ouvrirChat(r.getLouageId());
            rafraichir();
        });
    }

    private void confirmerPresence() {
        if (reservation == null) { notifier("Aucune réservation en cours"); return; }
        String id = reservation.getId();
        async(() -> { station.confirmerPresence(id); return id; }, x -> { });
    }

    private void annuler() {
        if (reservation == null) { notifier("Aucune réservation en cours"); return; }
        String id = reservation.getId();
        async(() -> { station.annulerReservation(id); return id; }, x -> { reservation = null; rafraichir(); });
    }

    private void rafraichir() {
        String trajet = trajetBox.getValue();
        async(() -> station.getLouages(trajet), this::afficherLouages);
        async(() -> station.estimerAttenteMinutes(trajet), min -> attenteLabel.setText(
                min < 0 ? "Aucun louage en remplissage"
                        : min == 0 ? "Départ imminent !" : "Départ estimé dans ~" + min + " min"));
    }

    private void afficherLouages(List<Louage> louages) {
        louagesBox.getChildren().clear();
        for (Louage l : louages) {
            int occupees = Louage.CAPACITE - l.placesRestantes();
            ProgressBar barre = new ProgressBar(occupees / (double) Louage.CAPACITE);
            barre.setPrefWidth(260);
            VBox fiche = new VBox(4,
                    new Label(l.getId() + "  ·  " + l.getMatricule() + "  ·  " + l.getChauffeur()),
                    barre,
                    new Label(occupees + "/" + Louage.CAPACITE + " places  ·  " + l.getEtat()));
            fiche.setPadding(new Insets(8));
            fiche.setStyle("-fx-background-color: " + couleur(l.getEtat()) + "; -fx-background-radius: 8;");
            louagesBox.getChildren().add(fiche);
        }
    }

    // ---------- Chat TCP ----------

    private void ouvrirChat(String louageId) {
        if (chat != null) chat.close();
        chat = new ChatClient();
        chatArea.clear();
        try {
            chat.connecter(Config.HOST, Config.TCP_PORT, louageId, nom,
                    ligne -> Platform.runLater(() -> chatArea.appendText(ligne + "\n")));
        } catch (IOException e) {
            notifier("Chat indisponible : " + e.getMessage());
            chat = null;
        }
    }

    // ---------- Utilitaires ----------

    private String couleur(EtatLouage e) {
        switch (e) {
            case EN_REMPLISSAGE: return "#D5F5E3";
            case COMPLET:        return "#FDEBD0";
            case EN_ROUTE:       return "#D6EAF8";
            default:             return "#F2F3F4";
        }
    }

    private void notifier(String texte) {
        notifications.getItems().add(0, LocalTime.now().withNano(0) + "  " + texte);
    }

    /** Exécute un appel RMI hors du thread JavaFX, puis affiche le résultat dans le thread JavaFX. */
    private <T> void async(Callable<T> appel, Consumer<T> succes) {
        arrierePlan.submit(() -> {
            try {
                T resultat = appel.call();
                Platform.runLater(() -> succes.accept(resultat));
            } catch (ReservationException e) {
                Platform.runLater(() -> notifier("⚠ " + e.getMessage()));
            } catch (Exception e) {
                Platform.runLater(() -> notifier("Erreur réseau : " + e.getMessage()));
            }
        });
    }

    @Override
    public void stop() {
        if (chat != null) chat.close();
        if (jms != null) jms.close();
        arrierePlan.shutdownNow();
    }
}