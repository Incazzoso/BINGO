import java.awt.*;
import java.io.*;
import java.net.Socket;
import javax.swing.*;

public class BingoPlayerGUI extends JFrame {
    private BingoTicket ticket = new BingoTicket();
    private JLabel[][] gridLabels = new JLabel[3][9];
    private JLabel infoLabel, lastNumberLabel, balanceLabel, betLabel;
    private PrintWriter out;
    private String playerName;
    private String serverIP;
    private double myBalance = 500.0;
    private boolean isPlaying = false;
    private boolean roundCompleted = false;

    public BingoPlayerGUI(String ipAddress) {
        this.serverIP = ipAddress;
        
        playerName = JOptionPane.showInputDialog("Tuo Nome:");
        if(playerName == null || playerName.trim().isEmpty()) System.exit(0);

        setTitle("Giocatore: " + playerName + " (Connesso a " + serverIP + ")");
        setSize(850, 450);
        
        // --- FIX RAM: Chiusura forzata del processo ---
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                System.out.println("Chiusura Client...");
                System.exit(0); // Uccide thread di rete e libera RAM
            }
        });
        // ----------------------------------------------
        
        setLayout(new BorderLayout(10,10));
        
        // Caricamento Icona (se presente)
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("icon.png"));
            setIconImage(icon.getImage());
        } catch (Exception e) {}

        JPanel side = new JPanel(new GridLayout(3,1));
        side.setPreferredSize(new Dimension(150, 0));
        balanceLabel = new JLabel("Saldo: 500.00 €", SwingConstants.CENTER);
        betLabel = new JLabel("Puntata: 0 €", SwingConstants.CENTER);
        side.add(new JLabel(playerName, SwingConstants.CENTER));
        side.add(balanceLabel);
        side.add(betLabel);
        add(side, BorderLayout.WEST);

        JPanel head = new JPanel(new GridLayout(2,1));
        head.setBackground(new Color(41, 128, 185));
        lastNumberLabel = new JLabel("-", SwingConstants.CENTER);
        lastNumberLabel.setFont(new Font("Arial", Font.BOLD, 50));
        lastNumberLabel.setForeground(Color.WHITE);
        head.add(new JLabel("ULTIMO ESTRATTO", SwingConstants.CENTER)).setForeground(Color.WHITE);
        head.add(lastNumberLabel);
        add(head, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(3,9,5,5));
        for(int r=0; r<3; r++){
            for(int c=0; c<9; c++){
                JLabel l = new JLabel("", SwingConstants.CENTER);
                l.setOpaque(true);
                l.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                l.setFont(new Font("Arial", Font.BOLD, 20));
                gridLabels[r][c] = l;
                grid.add(l);
            }
        }
        refreshUI();
        add(grid, BorderLayout.CENTER);

        infoLabel = new JLabel("Tentativo di connessione a " + serverIP + "...", SwingConstants.CENTER);
        add(infoLabel, BorderLayout.SOUTH);

        setVisible(true);
        new Thread(this::connect).start();
    }

    private void connect() {
        try (Socket s = new Socket(serverIP, 15244)) {
            SwingUtilities.invokeLater(() -> infoLabel.setText("Connesso al Banco!"));
            out = new PrintWriter(s.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            
            out.println("LOGIN:" + playerName + ":" + myBalance);

            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.startsWith("BET_REQUEST:")) {
                    int amt = Integer.parseInt(msg.split(":")[1]);
                    SwingUtilities.invokeLater(() -> askBet(amt));
                } else if (msg.startsWith("WINNER_EVENT:")) {
                    if (roundCompleted) continue;
                    roundCompleted = true;
                    String[] parts = msg.split(":");
                    String winnerName = parts[1];
                    double prize = Double.parseDouble(parts[2]);
                    SwingUtilities.invokeLater(() -> {
                        if (winnerName.contains(playerName)) {
                            myBalance += prize;
                            updateBalanceUI();
                            JOptionPane.showMessageDialog(this, "HAI VINTO! Premio: " + String.format("%.2f", prize) + "€");
                        } else {
                            JOptionPane.showMessageDialog(this, "Ha vinto " + winnerName);
                        }
                    });
                } else if (msg.startsWith("PAUSA_CINQUINA:")) {
                    String[] parts = msg.split(":");
                    SwingUtilities.invokeLater(() -> {
                        if(parts[1].equals(playerName)) {
                            double p = Double.parseDouble(parts[2]);
                            myBalance += p;
                            updateBalanceUI();
                            JOptionPane.showMessageDialog(this, "HAI FATTO CINQUINA! Vinti: " + String.format("%.2f", p) + "€");
                        } else {
                            infoLabel.setText("CINQUINA DI " + parts[1]);
                        }
                    });
                } else if (msg.equals("RESET_GAME")) {
                    ticket = new BingoTicket();
                    isPlaying = false;
                    roundCompleted = false;
                    SwingUtilities.invokeLater(() -> {
                        lastNumberLabel.setText("-");
                        betLabel.setText("Puntata: 0 €");
                        refreshUI();
                    });
                } else if (msg.startsWith("POT_UPDATE:")) {
                    String pot = msg.split(":")[1];
                    SwingUtilities.invokeLater(() -> infoLabel.setText("Montepremi: " + pot + " €"));
                } else {
                    try {
                        int n = Integer.parseInt(msg);
                        if(isPlaying) {
                            ticket.eliminateNumber(n);
                            SwingUtilities.invokeLater(() -> {
                                lastNumberLabel.setText(""+n);
                                refreshUI();
                                if(ticket.checkBingo()) out.println("BINGO:"+playerName);
                                else if(ticket.checkCinquina()) out.println("CINQUINA:"+playerName);
                            });
                        }
                    } catch(Exception e) {}
                }
            }
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                infoLabel.setText("Errore: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Impossibile connettersi al Server " + serverIP);
            });
        }
    }

    private void askBet(int amt) {
        int res = JOptionPane.showConfirmDialog(this, "Vuoi puntare " + amt + "€?", "Puntata", JOptionPane.YES_NO_OPTION);
        if(res == JOptionPane.YES_OPTION && myBalance >= amt) {
            myBalance -= amt;
            isPlaying = true;
            out.println("JOIN:" + playerName + ":" + myBalance);
            betLabel.setText("Puntata: " + amt + " €");
            updateBalanceUI();
        }
    }

    private void updateBalanceUI() {
        balanceLabel.setText("Saldo: " + String.format("%.2f", myBalance) + " €");
    }

    private void refreshUI() {
        for(int r=0; r<3; r++){
            for(int c=0; c<9; c++){
                int v = ticket.getSlotValue(r,c);
                JLabel l = gridLabels[r][c];
                if(v == 0) { l.setText(""); l.setBackground(Color.LIGHT_GRAY); }
                else if(v == -1) { l.setText("X"); l.setBackground(Color.RED); l.setForeground(Color.WHITE); }
                else { l.setText(""+v); l.setBackground(Color.WHITE); l.setForeground(Color.BLACK); }
            }
        }
    }

    public static void main(String[] args) { new BingoPlayerGUI("localhost"); }
}