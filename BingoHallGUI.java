import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

public class BingoHallGUI extends JFrame {
    private JButton startButton, resetButton;
    private JComboBox<Integer> betSelector;
    private JLabel statusLabel, potLabel, ipDisplayLabel;
    private JPanel numbersPanel;
    private JLabel[] numberLabels = new JLabel[91];
    
    // Tabella Giocatori
    private JTable playersTable;
    private DefaultTableModel playersModel;

    private final List<ClientHandler> playerHandlers = Collections.synchronizedList(new ArrayList<>());
    private BingoNumberGenerator generator = new BingoNumberGenerator();
    private Timer gameTimer;
    private boolean gameRunning = false;
    private boolean cinquinaAssegnata = false; 
    private boolean gameEnded = false; 
    private double totalPot = 0;
    private int currentBetAmount = 10;
    private final List<String> currentWinners = Collections.synchronizedList(new ArrayList<>());

    public BingoHallGUI() {
        // Recupero IP per mostrarlo all'utente
        String myIP = "Sconosciuto";
        try { myIP = InetAddress.getLocalHost().getHostAddress(); } catch (Exception e) {}
        
        setTitle("BINGO HALL - GOLD EDITION");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- TOP PANEL (IP e Controlli) ---
        JPanel topContainer = new JPanel(new BorderLayout());
        
        // Pannello IP (Giallo per risaltare)
        JPanel ipPanel = new JPanel();
        ipPanel.setBackground(Color.YELLOW);
        ipDisplayLabel = new JLabel("FAI CONNETTERE GLI AMICI A QUESTO IP: " + myIP);
        ipDisplayLabel.setFont(new Font("Arial", Font.BOLD, 16));
        ipPanel.add(ipDisplayLabel);
        topContainer.add(ipPanel, BorderLayout.NORTH);

        JPanel controlsPanel = new JPanel(new GridLayout(2, 1));
        JPanel r1 = new JPanel();
        betSelector = new JComboBox<>(new Integer[]{0, 10, 20, 50, 100}); // 0 per partite gratis
        startButton = new JButton("1. RICHIEDI PUNTATE");
        resetButton = new JButton("RESET / PROSSIMA MANO");
        resetButton.setEnabled(false);
        r1.add(new JLabel("Costo Cartella (€): ")); r1.add(betSelector); r1.add(startButton); r1.add(resetButton);

        JPanel r2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 5));
        statusLabel = new JLabel("Giocatori: 0");
        potLabel = new JLabel("MONTEPREMI: 0.00 €");
        potLabel.setFont(new Font("Arial", Font.BOLD, 22));
        potLabel.setForeground(new Color(0, 100, 0)); // Verde scuro
        r2.add(statusLabel); r2.add(potLabel);

        controlsPanel.add(r1); controlsPanel.add(r2);
        topContainer.add(controlsPanel, BorderLayout.CENTER);
        
        add(topContainer, BorderLayout.NORTH);

        // --- LEFT PANEL (LISTA GIOCATORI) ---
        String[] columnNames = {"Giocatore", "Saldo (€)", "Stato"};
        playersModel = new DefaultTableModel(columnNames, 0);
        playersTable = new JTable(playersModel);
        playersTable.setEnabled(false); 
        JScrollPane scrollPane = new JScrollPane(playersTable);
        scrollPane.setPreferredSize(new Dimension(300, 0));
        scrollPane.setBorder(BorderFactory.createTitledBorder("Sala Giochi"));
        add(scrollPane, BorderLayout.WEST);

        // --- CENTER PANEL (TABELLONE) ---
        numbersPanel = new JPanel(new GridLayout(9, 10, 2, 2));
        initializeTabellone();
        add(numbersPanel, BorderLayout.CENTER);

        // Avvio Server
        new Thread(this::setupServer).start();

        // Logica Pulsanti
        startButton.addActionListener(e -> {
            if (startButton.getText().contains("RICHIEDI")) {
                currentBetAmount = (Integer) betSelector.getSelectedItem();
                totalPot = 0;
                currentWinners.clear();
                broadcast("BET_REQUEST:" + currentBetAmount);
                startButton.setText("2. AVVIA ESTRAZIONE");
            } else {
                // Controllo se ci sono giocatori
                if (playerHandlers.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Nessun giocatore connesso!");
                    return;
                }
                gameRunning = true;
                cinquinaAssegnata = false;
                gameEnded = false;
                startButton.setEnabled(false);
                resetButton.setEnabled(false);
                startGameLoop();
            }
        });

        resetButton.addActionListener(e -> resetGame());
        setVisible(true);
    }

    private void updatePlayerList() {
        SwingUtilities.invokeLater(() -> {
            playersModel.setRowCount(0);
            synchronized(playerHandlers) {
                for (ClientHandler h : playerHandlers) {
                    if (h.name != null) {
                        String stato = h.hasPaid ? "PAGATO" : "In attesa";
                        playersModel.addRow(new Object[]{
                            h.name, 
                            String.format("%.2f", h.balance), 
                            stato
                        });
                    }
                }
            }
            statusLabel.setText("Giocatori Connessi: " + playerHandlers.size());
        });
    }

    private void startGameLoop() {
        gameTimer = new Timer(2000, e -> { // 2 secondi tra un numero e l'altro
            if (gameEnded) return;
            int num = generator.nextInt();
            if (num == 0) stopGame("Esauriti");
            else {
                updateTableUI(num);
                broadcast(String.valueOf(num));
            }
        });
        gameTimer.start();
    }

    private synchronized void stopGame(String reason) {
        if (gameEnded) return;
        gameEnded = true;
        if (gameTimer != null) gameTimer.stop();
        gameRunning = false;
        
        if (!currentWinners.isEmpty()) {
            double prize = totalPot; 
            double individualPrize = prize / currentWinners.size();
            String names = String.join(" & ", currentWinners);
            
            synchronized(playerHandlers) {
                for(ClientHandler h : playerHandlers) {
                    for(String winner : currentWinners) {
                        if(h.name.equals(winner)) {
                            h.balance += individualPrize;
                        }
                    }
                }
            }
            updatePlayerList();
            broadcast("WINNER_EVENT:" + names + ":" + individualPrize);
            statusLabel.setText("Vittoria: " + names);
        } else {
            broadcast("WINNER_EVENT:Nessuno:0");
        }
        
        resetButton.setEnabled(true);
    }

    private void resetGame() {
        generator.reset();
        currentWinners.clear();
        totalPot = 0;
        cinquinaAssegnata = false;
        gameEnded = false;
        potLabel.setText("MONTEPREMI: 0.00 €");
        
        synchronized(playerHandlers) {
            for(ClientHandler h : playerHandlers) {
                h.hasPaid = false;
            }
        }
        updatePlayerList();
        initializeTabellone();
        broadcast("RESET_GAME");
        startButton.setText("1. RICHIEDI PUNTATE");
        startButton.setEnabled(true);
        resetButton.setEnabled(false);
    }

    private void setupServer() {
        try (ServerSocket ss = new ServerSocket(15244)) {
            while (true) {
                Socket s = ss.accept();
                ClientHandler h = new ClientHandler(s);
                playerHandlers.add(h);
                new Thread(h).start();
                updatePlayerList();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void broadcast(String m) {
        synchronized(playerHandlers) {
            playerHandlers.forEach(h -> h.out.println(m));
        }
    }

    private void updateTableUI(int n) {
        SwingUtilities.invokeLater(() -> {
            numberLabels[n].setBackground(Color.RED);
            numberLabels[n].setForeground(Color.WHITE);
            statusLabel.setText("Estratto: " + n);
        });
    }

    private void initializeTabellone() {
        numbersPanel.removeAll();
        for (int i = 1; i <= 90; i++) {
            JLabel l = new JLabel(String.valueOf(i), SwingConstants.CENTER);
            l.setOpaque(true);
            l.setBackground(Color.WHITE);
            l.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            numberLabels[i] = l;
            numbersPanel.add(l);
        }
        numbersPanel.revalidate();
        numbersPanel.repaint();
    }

    class ClientHandler implements Runnable {
        PrintWriter out; BufferedReader in; 
        String name; 
        double balance = 500.0; // Saldo iniziale
        boolean hasPaid = false;

        ClientHandler(Socket s) throws IOException {
            out = new PrintWriter(s.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        }

        public void run() {
            try {
                String m;
                while ((m = in.readLine()) != null) {
                    if (m.startsWith("LOGIN:")) {
                        name = m.split(":")[1];
                        updatePlayerList();
                    } else if (m.startsWith("JOIN:")) {
                        // Il cliente accetta la puntata
                        if (!hasPaid) {
                            balance -= currentBetAmount;
                            totalPot += currentBetAmount;
                            hasPaid = true;
                            SwingUtilities.invokeLater(() -> potLabel.setText("MONTEPREMI: " + totalPot + " €"));
                            broadcast("POT_UPDATE:" + totalPot);
                            updatePlayerList();
                        }
                    } else if (m.startsWith("CINQUINA:")) {
                        synchronized(BingoHallGUI.this) {
                            if (!cinquinaAssegnata && !gameEnded) {
                                cinquinaAssegnata = true;
                                double prize = totalPot * 0.20; // 20% per la cinquina
                                totalPot -= prize;
                                balance += prize; 
                                updatePlayerList();
                                broadcast("PAUSA_CINQUINA:" + name + ":" + prize);
                                SwingUtilities.invokeLater(() -> potLabel.setText("MONTEPREMI: " + totalPot + " €"));
                                
                                // Pausa scenica
                                if(gameTimer != null) gameTimer.stop();
                                new Timer(5000, e -> { if(gameRunning && !gameEnded) gameTimer.start(); }).start();
                            }
                        }
                    } else if (m.startsWith("BINGO:")) {
                        synchronized(BingoHallGUI.this) {
                            if(!gameEnded) {
                                currentWinners.add(name);
                                // Aspetta un attimo per vedere se altri fanno bingo contemporaneamente
                                new Timer(500, e -> stopGame("Bingo")).start();
                            }
                        }
                    }
                }
            } catch (IOException e) { 
                playerHandlers.remove(this); 
                updatePlayerList();
            }
        }
    }
}