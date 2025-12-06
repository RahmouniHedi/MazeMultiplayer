package common;

public class Constants {
    public static final int RMI_PORT = 1099;
    public static final int UDP_PORT = 9876;
    public static final String RMI_ID = "MazeService";
    public static final String SERVER_IP = "192.168.1.108";
    public static final String BROKER_URL = "tcp://" + SERVER_IP + ":61616";
    public static final String CHAT_TOPIC = "maze.chat";
    public static final String EVENT_TOPIC = "maze.events";

    // IMPORTANT : Doit être un nombre IMPAIR pour l'algorithme de génération !
    public static final int MAZE_SIZE = 41;
    public static final int CELL_SIZE = 15; // Plus petit pour afficher plus de détails
}