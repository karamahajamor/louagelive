package tn.fss.louagelive.common;

public enum Ville {
    SFAX(34.7406, 10.7603), TUNIS(36.8065, 10.1815), SOUSSE(35.8256, 10.6084), GABES(33.8815, 10.0982);

    public final double lat;
    public final double lon;

    Ville(double lat, double lon) { this.lat = lat; this.lon = lon; }

    public static Ville depart(String trajet)  { return valueOf(trajet.split("-")[0]); }
    public static Ville arrivee(String trajet) { return valueOf(trajet.split("-")[1]); }
}