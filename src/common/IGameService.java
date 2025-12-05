package common;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IGameService extends Remote {
    int login(String username) throws RemoteException;
    int[][] getMaze() throws RemoteException;

    // NOUVELLE MÉTHODE : Pour savoir si on joue en 21x21 ou 61x61
    int getMazeSize() throws RemoteException;
}