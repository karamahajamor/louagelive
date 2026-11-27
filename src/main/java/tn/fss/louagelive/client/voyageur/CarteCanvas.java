package tn.fss.louagelive.client.voyageur;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import tn.fss.louagelive.common.Ville;

import java.util.HashMap;
import java.util.Map;

public class CarteCanvas extends Canvas {
    private static final double LAT_MIN = 33.5, LAT_MAX = 37.2, LON_MIN = 9.4, LON_MAX = 11.4;
    private final Map<String, double[]> positions = new HashMap<>();
    private final Map<String, String> etats = new HashMap<>();

    public CarteCanvas(double largeur, double hauteur) {
        super(largeur, hauteur);
        dessiner();
    }

    public void majPosition(String id, double lat, double lon, String etat) {
        positions.put(id, new double[]{lat, lon});
        etats.put(id, etat);
        dessiner();
    }

    private double x(double lon) { return 30 + (lon - LON_MIN) / (LON_MAX - LON_MIN) * (getWidth() - 60); }
    private double y(double lat) { return 30 + (LAT_MAX - lat) / (LAT_MAX - LAT_MIN) * (getHeight() - 60); }

    private void dessiner() {
        GraphicsContext g = getGraphicsContext2D();
        g.setFill(Color.web("#EAF2F8"));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setStroke(Color.web("#B0BEC5"));
        g.setLineWidth(2);
        for (Ville v : new Ville[]{Ville.TUNIS, Ville.SOUSSE, Ville.GABES}) {
            g.strokeLine(x(Ville.SFAX.lon), y(Ville.SFAX.lat), x(v.lon), y(v.lat));
        }
        g.setFont(Font.font(13));
        for (Ville v : Ville.values()) {
            g.setFill(Color.web("#37474F"));
            g.fillOval(x(v.lon) - 5, y(v.lat) - 5, 10, 10);
            g.fillText(v.name(), x(v.lon) + 8, y(v.lat) + 4);
        }
        for (Map.Entry<String, double[]> e : positions.entrySet()) {
            String etat = etats.get(e.getKey());
            if (!"EN_ROUTE".equals(etat) && !"ARRIVE".equals(etat)) continue;   // seuls les louages partis
            double px = x(e.getValue()[1]), py = y(e.getValue()[0]);
            g.setFill("EN_ROUTE".equals(etat) ? Color.web("#E67E22") : Color.web("#27AE60"));
            g.fillRoundRect(px - 7, py - 7, 14, 14, 4, 4);
            g.setFill(Color.BLACK);
            g.fillText(e.getKey(), px + 10, py - 6);
        }
    }
}