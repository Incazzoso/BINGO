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
    private JLabel statusLabel, potLabel;
    private JPanel numbersPanel;
    private JLabel[] numberLabels = new JLabel[91];
    
    // Lista Giocatori (GUI)
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
        // Recupero IP
        String myIP = "Sconosciuto";
        try { myIP = InetAddress.getLocalHost().getHostAddress(); } catch (Exception e) {}
        
        setTitle("BINGO SERVER - HOST: " + myIP);
        setSize(900, 800);
        
        // --- FIX RAM: Chiusura forzata del processo ---
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                System.out.println("Spegnimento Server in corso...");
                System.exit(0); // Uccide tutti i thread e libera la RAM
            }
        });
        // ----------------------------------------------

        setLayout(new BorderLayout());
        
        // Caricamento Icona (se presente)
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("icon.png"));
            setIconImage(icon.getImage());
        } catch (Exception e) {}

        // --- TOP PANEL ---
        JPanel top = new JPanel(new GridLayout(2, 1));
        JPanel r1 = new JPanel();
        betSelector = new JComboBox<>(new Integer[]{10, 20, 50, 100});
        startButton = new JButton("1. RICHIEDI PUNTATE");
        resetButton = new JButton("RESET / PROSSIMA MANO");
        resetButton.setEnabled(false);
        r1.add(new JLabel("Puntata: ")); r1.add(betSelector); r1.add(startButton); r1.add(resetButton);

        JPanel r2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 5));
        statusLabel = new JLabel("Giocatori: 0");
        potLabel = new JLabel("MONTEPREMI: 0.00 €");
        potLabel.setFont(new Font("Arial", Font.BOLD, 22));
        r2.add(statusLabel); r2.add(potLabel);

        top.add(r1); top.add(r2);
        add(top, BorderLayout.NORTH);

        // --- LEFT PANEL (LISTA GIOCATORI) ---
        String[] columnNames = {"Giocatore", "Saldo (€)", "Puntata (€)"};
        playersModel = new DefaultTableModel(columnNames, 0);
        playersTable = new JTable(playersModel);
        playersTable.setEnabled(false); // Sola lettura
        JScrollPane scrollPane = new JScrollPane(playersTable);
        scrollPane.setPreferredSize(new Dimension(250, 0));
        scrollPane.setBorder(BorderFactory.createTitledBorder("Stato Giocatori"));
        add(scrollPane, BorderLayout.WEST);

        // --- CENTER PANEL (TABELLONE) ---
        numbersPanel = new JPanel(new GridLayout(9, 10, 2, 2));
        initializeTabellone();
        add(numbersPanel, BorderLayout.CENTER);

        // --- BOTTOM PANEL ---
        JPanel ipPanel = new JPanel();
        ipPanel.setBackground(Color.YELLOW);
        ipPanel.add(new JLabel("FAI CONNETTERE GLI AMICI A: " + myIP));
        add(ipPanel, BorderLayout.SOUTH);

        new Thread(this::setupServer).start();

        startButton.addActionListener(e -> {
            if (startButton.getText().contains("RICHIEDI")) {
                currentBetAmount = (Integer) betSelector.getSelectedItem();
                totalPot = 0;
                currentWinners.clear();
                broadcast("BET_REQUEST:" + currentBetAmount);
                startButton.setText("2. AVVIA ESTRAZIONE");
            } else {
                if (totalPot == 0) {
                    JOptionPane.showMessageDialog(this, "Nessun giocatore ha puntato!");
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
                        playersModel.addRow(new Object[]{
                            h.name, 
                            String.format("%.2f", h.balance), 
                            h.currentBet > 0 ? h.currentBet : "-"
                        });
                    }
                }
            }
            statusLabel.setText("Giocatori Connessi: " + playerHandlers.size());
        });
    }

    private void startGameLoop() {
        gameTimer = new Timer(1500, e -> {
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
            statusLabel.setText("Vinto da: " + names + " (" + String.format("%.2f", individualPrize) + "€)");
        } else {
            broadcast("WINNER_EVENT:Nessuno:0");
            statusLabel.setText("Partita terminata senza vincitori.");
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
                h.currentBet = 0;
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
        } catch (IOException e) {}
    }

    private void broadcast(String m) {
        synchronized(playerHandlers) {
            playerHandlers.forEach(h -> h.out.println(m));
        }
    }

    private void updateTableUI(int n) {
        SwingUtilities.invokeLater(() -> {
            numberLabels[n].setBackground(Color.GREEN);
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
        double balance = 0;
        int currentBet = 0;

        ClientHandler(Socket s) throws IOException {
            out = new PrintWriter(s.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        }

        public void run() {
            try {
                String m;
                while ((m = in.readLine()) != null) {
                    if (m.startsWith("LOGIN:")) {
                        String[] parts = m.split(":");
                        name = parts[1];
                        balance = Double.parseDouble(parts[2]);
                        updatePlayerList();
                    
                    } else if (m.startsWith("JOIN:")) {
                        String[] parts = m.split(":");
                        balance = Double.parseDouble(parts[2]);
                        currentBet = currentBetAmount;
                        
                        totalPot += currentBetAmount;
                        SwingUtilities.invokeLater(() -> potLabel.setText("MONTEPREMI: " + totalPot + " €"));
                        broadcast("POT_UPDATE:" + totalPot);
                        updatePlayerList();

                    } else if (m.startsWith("CINQUINA:")) {
                        synchronized(BingoHallGUI.this) {
                            if (!cinquinaAssegnata && !gameEnded) {
                                cinquinaAssegnata = true;
                                double prize = totalPot / 10.0;
                                totalPot -= prize;
                                
                                balance += prize; 
                                updatePlayerList();

                                SwingUtilities.invokeLater(() -> potLabel.setText("MONTEPREMI: " + totalPot + " €"));
                                broadcast("PAUSA_CINQUINA:" + name + ":" + prize);
                                
                                if(gameTimer != null) gameTimer.stop();
                                new Timer(10000, e -> { if(gameRunning && !gameEnded) gameTimer.start(); }).start();
                            }
                        }
                    } else if (m.startsWith("BINGO:")) {
                        synchronized(BingoHallGUI.this) {
                            if(!gameEnded) {
                                currentWinners.add(name);
                                new Timer(300, e -> stopGame("Bingo")).start();
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

    public static void main(String[] args) { new BingoHallGUI(); }
}