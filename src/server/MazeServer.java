package server;

import common.*;
import javax.jms.*;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.activemq.ActiveMQConnectionFactory;
import java.util.Scanner;

public class MazeServer extends UnicastRemoteObject implements IGameService {

    private static ConcurrentHashMap<Integer, java.awt.Point> players = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Integer, InetAddress> clientIPs = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Integer, Integer> clientPorts = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Integer, String> playerNames = new ConcurrentHashMap<>();
    private int currentMazeSize;
    private int[][] maze;
    private int nextId = 1;
    private DatagramSocket udpSocket;

    // JMS Logic
    private Connection jmsConnection;
    private Session jmsSession;
    private MessageProducer eventProducer;

    public MazeServer(int size) throws Exception {
        super();
        this.currentMazeSize = size; // On stocke la taille choisie
        generateMazeRecursive();
        initUDP();
        initJMS();
        System.out.println("[SERVEUR] Labyrinthe de taille " + size + "x" + size + " généré.");
    }
    @Override
    public int getMazeSize() throws RemoteException {
        return currentMazeSize;
    }
    // --- ALGORITHME DE GÉNÉRATION (Recursive Backtracker) ---
    private void generateMazeRecursive() {
        // 1. Initialisation (Mur partout)
        maze = new int[currentMazeSize][currentMazeSize];
        for (int i = 0; i < currentMazeSize; i++) {
            for (int j = 0; j < currentMazeSize; j++) {
                maze[i][j] = 1;
            }
        }

        // 2. Algorithme DFS (Génère le chemin unique)
        carve(1, 1);

        // 3. --- NOUVEAU : AJOUT DE BOUCLES ---
        // On supprime environ 10% des murs restants pour créer des cycles
        addLoops(10);

        // 4. Sortie et Départ
        maze[currentMazeSize - 2][currentMazeSize - 2] = 9;
        maze[1][1] = 0;
    }
    private void addLoops(int percentage) {
        // Parcourt tout le labyrinthe (sauf les bords extérieurs)
        for (int i = 2; i < currentMazeSize - 2; i++) {
            for (int j = 2; j < currentMazeSize - 2; j++) {

                // Si c'est un mur (1)
                if (maze[i][j] == 1) {

                    // Vérifie si ce mur sépare deux cases vides verticalement ou horizontalement
                    boolean separatesVertical = (maze[i-1][j] == 0 && maze[i+1][j] == 0);
                    boolean separatesHorizontal = (maze[i][j-1] == 0 && maze[i][j+1] == 0);

                    if (separatesVertical || separatesHorizontal) {
                        // On lance un dé pour savoir si on casse ce mur
                        if (Math.random() * 100 < percentage) {
                            maze[i][j] = 0; // Le mur devient un passage
                        }
                    }
                }
            }
        }
    }

    private void carve(int x, int y) {
        maze[x][y] = 0;
        ArrayList<Integer> dirs = new ArrayList<>();
        dirs.add(0); dirs.add(1); dirs.add(2); dirs.add(3);
        Collections.shuffle(dirs);

        for (int dir : dirs) {
            int nx = x, ny = y;
            switch (dir) {
                case 0: ny -= 2; break;
                case 1: ny += 2; break;
                case 2: nx -= 2; break;
                case 3: nx += 2; break;
            }
            // Utilisez currentMazeSize ici aussi !
            if (nx > 0 && nx < currentMazeSize - 1 && ny > 0 && ny < currentMazeSize - 1 && maze[nx][ny] == 1) {
                maze[(x + nx) / 2][(y + ny) / 2] = 0;
                carve(nx, ny);
            }
        }
    }
    // ---------------------------------------------------------

    private void initUDP() {
        new Thread(() -> {
            try {
                udpSocket = new DatagramSocket(Constants.UDP_PORT);
                byte[] buffer = new byte[1024];
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength());
                    processUdpMessage(msg, packet.getAddress(), packet.getPort());
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void initJMS() throws JMSException {
        ConnectionFactory factory = new ActiveMQConnectionFactory(Constants.BROKER_URL);
        jmsConnection = factory.createConnection();
        jmsConnection.start();
        jmsSession = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = jmsSession.createTopic(Constants.EVENT_TOPIC);
        eventProducer = jmsSession.createProducer(topic);
    }

    @Override
    public synchronized int login(String username) throws java.rmi.RemoteException {
        int id = nextId++;
        players.put(id, new java.awt.Point(1, 1));
        playerNames.put(id, username);
        try {
            TextMessage msg = jmsSession.createTextMessage("SERVEUR: " + username + " est entré dans le labyrinthe.");
            eventProducer.send(msg);
        } catch (JMSException e) { e.printStackTrace(); }
        System.out.println("Nouveau joueur: " + username + " (ID: " + id + ")");
        return id;
    }

    @Override
    public int[][] getMaze() throws java.rmi.RemoteException {
        return maze;
    }

    private void processUdpMessage(String msg, InetAddress ip, int port) {
        String[] parts = msg.split(";");
        if (parts[0].equals("MOVE")) {
            int id = Integer.parseInt(parts[1]);
            String dir = parts[2];

            // Enregistrement continu de l'IP/Port
            clientIPs.put(id, ip);
            clientPorts.put(id, port);

            java.awt.Point p = players.get(id);
            if (p == null) return; // Joueur pas encore login

            int newX = p.x;
            int newY = p.y;

            switch(dir) {
                case "UP": newY--; break;
                case "DOWN": newY++; break;
                case "LEFT": newX--; break;
                case "RIGHT": newX++; break;
            }

            // Vérification des collisions stricte
            if (newX >= 0 && newX < Constants.MAZE_SIZE && newY >= 0 && newY < Constants.MAZE_SIZE) {
                if (maze[newX][newY] != 1) { // 1 = Mur
                    p.x = newX;
                    p.y = newY;
                    players.put(id, p);
                    broadcastPosition(id, p.x, p.y);

                    if (maze[newX][newY] == 9) { // 9 = Sortie
                        broadcastWin(id);
                    }
                }
            }
        }
    }

    private void broadcastPosition(int id, int x, int y) {
        String msg = "POS;" + id + ";" + x + ";" + y;
        byte[] data = msg.getBytes();
        clientIPs.forEach((pid, ip) -> {
            try {
                int port = clientPorts.get(pid);
                DatagramPacket packet = new DatagramPacket(data, data.length, ip, port);
                udpSocket.send(packet);
            } catch (Exception e) {}
        });
    }

    private void broadcastWin(int id) {
        try {
            String username = playerNames.get(id);
            TextMessage msg = jmsSession.createTextMessage("VICTOIRE ! Le joueur " + username + " a trouvé la sortie !");
            eventProducer.send(msg);
        } catch (Exception e) {}
    }

    public static void main(String[] args) {
        try {
            // MENU DE SÉLECTION
            Scanner scanner = new Scanner(System.in);
            System.out.println("=== CHOISISSEZ LA DIFFICULTÉ ===");
            System.out.println("1. Facile (Petit - 21x21)");
            System.out.println("2. Moyen  (Normal - 41x41)");
            System.out.println("3. Difficile (Grand - 61x61)");
            System.out.print("Votre choix : ");

            int choice = scanner.nextInt();
            int size = 41; // Défaut

            if (choice == 1) size = 21; // DOIT être impair
            if (choice == 2) size = 41;
            if (choice == 3) size = 61;

            Registry registry = LocateRegistry.createRegistry(Constants.RMI_PORT);
            // On passe la taille au constructeur
            registry.rebind(Constants.RMI_ID, new MazeServer(size));

            System.out.println("Serveur prêt en mode " + (choice==1?"FACILE":(choice==2?"MOYEN":"DIFFICILE")));

        } catch (Exception e) { e.printStackTrace(); }
    }
}