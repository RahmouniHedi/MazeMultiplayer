package client;

import common.*;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;
import javax.jms.*;

public class MazeClient extends JFrame {

    private IGameService server;
    private int myId;
    private int[][] maze;
    private Map<Integer, Point> otherPlayers = new HashMap<>();
    private int currentMazeSize;
    private DatagramSocket udpSocket;
    private InetAddress serverAddress;

    private Session jmsSession;
    private MessageProducer chatProducer;

    private JPanel gamePanel;
    private JTextArea chatArea;
    private JTextField chatInput;

    // --- COULEURS DU DESIGN ---
    private final Color WALL_COLOR = new Color(44, 62, 80);    // Bleu nuit foncé
    private final Color FLOOR_COLOR = new Color(236, 240, 241); // Blanc cassé
    private final Color MY_PLAYER_COLOR = new Color(52, 152, 219); // Bleu clair
    private final Color OTHER_PLAYER_COLOR = new Color(231, 76, 60); // Rouge
    private final Color EXIT_COLOR = new Color(46, 204, 113); // Vert émeraude

    public MazeClient(String username) {
        super("Labyrinthe Distribué - " + username);
        try {
            Registry registry = LocateRegistry.getRegistry(Constants.SERVER_IP, Constants.RMI_PORT);
            server = (IGameService) registry.lookup(Constants.RMI_ID);

            // 1. D'abord, on demande la taille du labyrinthe
            // Dans le constructeur, remplacez les appels getMaze/getMazeSize par :
            MazeState state = server.getMazeState();
            this.currentMazeSize = state.getSize();
            this.maze = state.getGrid();
            System.out.println("Mode de jeu : " + state.getDifficultyName()); // Preuve d'objet complexe

            // 2. Ensuite on se connecte et on récupère le labyrinthe
            myId = server.login(username);


            // ... reste du code (UDP, JMS) ...
            otherPlayers.put(myId, new Point(1, 1));
            udpSocket = new DatagramSocket();
            serverAddress = InetAddress.getByName(Constants.SERVER_IP);
            startUdpListener();
            setupJMS(username);

            initGUI(); // initGUI utilisera currentMazeSize maintenant
            sendMove("NONE");

        } catch (Exception e) { /* ... */ }
    }

    private void initGUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- ZONE DE JEU AMÉLIORÉE ---
        gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawBeautifulMaze((Graphics2D) g); // Utilisation de Graphics2D
            }
        };
        // Calculer la taille idéale de la fenêtre
        int dim = currentMazeSize * Constants.CELL_SIZE;
        gamePanel.setPreferredSize(new Dimension(dim, dim));
        gamePanel.setFocusable(true);
        gamePanel.setBackground(WALL_COLOR); // Fond par défaut

        gamePanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP) sendMove("UP");
                if (e.getKeyCode() == KeyEvent.VK_DOWN) sendMove("DOWN");
                if (e.getKeyCode() == KeyEvent.VK_LEFT) sendMove("LEFT");
                if (e.getKeyCode() == KeyEvent.VK_RIGHT) sendMove("RIGHT");
            }
        });

        // --- ZONE DE CHAT STYLISÉE ---
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        chatPanel.setBackground(new Color(220, 220, 220));

        chatArea = new JTextArea(6, 20);
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        chatArea.setBackground(new Color(250, 250, 250));
        JScrollPane scroll = new JScrollPane(chatArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Discussion Temps Réel"));

        chatInput = new JTextField();
        chatInput.setFont(new Font("SansSerif", Font.PLAIN, 14));
        chatInput.addActionListener(e -> sendChatMessage(chatInput.getText()));

        chatPanel.add(scroll, BorderLayout.CENTER);
        chatPanel.add(chatInput, BorderLayout.SOUTH);

        add(gamePanel, BorderLayout.CENTER);
        add(chatPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null); // Centrer à l'écran
        setVisible(true);
        gamePanel.requestFocus();
    }

    private void drawBeautifulMaze(Graphics2D g2) {
        // Activer l'antialiasing pour des cercles lisses
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dessiner le sol et les murs
        for (int i = 0; i < currentMazeSize; i++) {
            for (int j = 0; j < currentMazeSize; j++) {
                int x = i * Constants.CELL_SIZE;
                int y = j * Constants.CELL_SIZE;

                if (maze[i][j] == 0) {
                    // Sol (Chemin)
                    g2.setColor(FLOOR_COLOR);
                    g2.fillRect(x, y, Constants.CELL_SIZE, Constants.CELL_SIZE);
                } else if (maze[i][j] == 9) {
                    // Sortie (Exit) - Effet brillant
                    g2.setColor(EXIT_COLOR);
                    g2.fillRect(x, y, Constants.CELL_SIZE, Constants.CELL_SIZE);
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2));
                    g2.drawRect(x+2, y+2, Constants.CELL_SIZE-4, Constants.CELL_SIZE-4);
                }
                // Les murs (1) sont déjà la couleur de fond (WALL_COLOR) pour optimiser
            }
        }

        // Dessiner les joueurs (Ronds avec bordures blanches)
        for (Map.Entry<Integer, Point> entry : otherPlayers.entrySet()) {
            Point p = entry.getValue();
            int px = p.x * Constants.CELL_SIZE;
            int py = p.y * Constants.CELL_SIZE;

            if (entry.getKey() == myId) {
                g2.setColor(MY_PLAYER_COLOR);
            } else {
                g2.setColor(OTHER_PLAYER_COLOR);
            }

            // Cercle du joueur
            g2.fillOval(px + 2, py + 2, Constants.CELL_SIZE - 4, Constants.CELL_SIZE - 4);

            // Bordure blanche pour le contraste
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1));
            g2.drawOval(px + 2, py + 2, Constants.CELL_SIZE - 4, Constants.CELL_SIZE - 4);
        }
    }

    private void sendMove(String dir) {
        try {
            String msg = "MOVE;" + myId + ";" + dir;
            byte[] data = msg.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, Constants.UDP_PORT);
            udpSocket.send(packet);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void startUdpListener() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength());
                    String[] parts = msg.split(";");
                    if (parts[0].equals("POS")) {
                        int id = Integer.parseInt(parts[1]);
                        int x = Integer.parseInt(parts[2]);
                        int y = Integer.parseInt(parts[3]);
                        otherPlayers.put(id, new Point(x, y));
                        gamePanel.repaint(); // Redessiner fluidement
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void setupJMS(String username) throws JMSException {
        ConnectionFactory factory = new ActiveMQConnectionFactory(Constants.BROKER_URL);

        // CORRECTION pour JMS 1.1 (Pas de try-with-resources)
        Connection connection = factory.createConnection();
        connection.start();

        jmsSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Topic chatTopic = jmsSession.createTopic(Constants.CHAT_TOPIC);
        chatProducer = jmsSession.createProducer(chatTopic);

        MessageConsumer chatConsumer = jmsSession.createConsumer(chatTopic);
        chatConsumer.setMessageListener(message -> {
            try {
                if (message instanceof TextMessage) {
                    chatArea.append(((TextMessage) message).getText() + "\n");
                    chatArea.setCaretPosition(chatArea.getDocument().getLength()); // Auto-scroll
                }
            } catch (JMSException e) { e.printStackTrace(); }
        });

        Topic eventTopic = jmsSession.createTopic(Constants.EVENT_TOPIC);
        MessageConsumer eventConsumer = jmsSession.createConsumer(eventTopic);
        eventConsumer.setMessageListener(message -> {
            try {
                if (message instanceof TextMessage) {
                    chatArea.append(">>> " + ((TextMessage) message).getText() + "\n");
                    chatArea.setCaretPosition(chatArea.getDocument().getLength());
                }
            } catch (JMSException e) { e.printStackTrace(); }
        });
    }

    private void sendChatMessage(String text) {
        try {
            if(!text.trim().isEmpty()){
                TextMessage msg = jmsSession.createTextMessage("[" + myId + "]: " + text);
                chatProducer.send(msg);
                chatInput.setText("");
            }
        } catch (JMSException e) { e.printStackTrace(); }
    }

    public static void main(String[] args) {
        String name = JOptionPane.showInputDialog("Entrez votre pseudo:");
        if (name != null && !name.isEmpty()) {
            // Activer l'accélération matérielle pour la fluidité
            System.setProperty("sun.java2d.opengl", "true");
            new MazeClient(name);
        } else {
            System.exit(0);
        }
    }
}