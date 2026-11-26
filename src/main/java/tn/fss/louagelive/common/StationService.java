package tn.fss.louagelive.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface StationService extends Remote {
    List<String> getTrajets() throws RemoteException;
    List<Louage> getLouages(String trajet) throws RemoteException;
    Louage getLouage(String louageId) throws RemoteException;
    Reservation reserverPlace(String voyageur, String trajet) throws RemoteException, ReservationException;
    void confirmerPresence(String reservationId) throws RemoteException, ReservationException;
    void annulerReservation(String reservationId) throws RemoteException, ReservationException;
    int estimerAttenteMinutes(String trajet) throws RemoteException;
}