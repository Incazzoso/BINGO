import java.awt.*;
import java.io.*;
import java.net.Socket;
import javax.swing.*;
import javax.swing.border.LineBorder;

public class BingoPlayerGUI extends JFrame {
    private BingoTicket ticket = new BingoTicket();
    private JLabel[][] gridLabels = new JLabel[3][9];
    private JLabel infoLabel, lastNumberLabel, balanceLabel, betLabel;
    private PrintWriter out;
    private String playerName;
    private String serverIP;
    private double myBalance = 500.0;
    private boolean isPlaying = false;

    public BingoPlayerGUI(String ipAddress) {
        this.serverIP = ipAddress;
        
        playerName = JOptionPane.showInputDialog("Come ti chiami?");
        if(playerName == null || playerName.trim().isEmpty()) System.exit(0);

        setTitle(playerName + " - Connesso a: " + serverIP);
        setSize(850, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10,10));

        // --- SIDE PANEL (Stats) ---
        JPanel side = new JPanel(new GridLayout(3,1));
        side.setPreferredSize(new Dimension(200, 0));
        side.setBackground(new Color(240, 240, 240));
        
        balanceLabel = new JLabel("Saldo: 500.00 €", SwingConstants.CENTER);
        balanceLabel.setFont(new Font("Arial", Font.BOLD, 16));
        
        betLabel = new JLabel("Puntata: 0 €", SwingConstants.CENTER);
        
        side.add(new JLabel("Giocatore: " + playerName, SwingConstants.CENTER));
        side.add(balanceLabel);
        side.add(betLabel);
        add(side, BorderLayout.WEST);

        // --- TOP PANEL (Last Number) ---
        JPanel head = new JPanel(new GridLayout(2,1));
        head.setBackground(new Color(41, 128, 185));
        lastNumberLabel = new JLabel("-", SwingConstants.CENTER);
        lastNumberLabel.setFont(new Font("Arial", Font.BOLD, 60));
        lastNumberLabel.setForeground(Color.WHITE);
        
        JLabel title = new JLabel("ULTIMO ESTRATTO", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        head.add(title);
        head.add(lastNumberLabel);
        add(head, BorderLayout.NORTH);

        // --- CENTER PANEL (Ticket Grid) ---
        JPanel grid = new JPanel(new GridLayout(3,9,5,5));
        grid.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        for(int r=0; r<3; r++){
            for(int c=0; c<9; c++){
                JLabel l = new JLabel("", SwingConstants.CENTER);
                l.setOpaque(true);
                l.setBorder(new LineBorder(Color.BLACK));
                l.setFont(new Font("Arial", Font.BOLD, 24));
                gridLabels[r][c] = l;
                grid.add(l);
            }
        }
        refreshTicketUI();
        add(grid, BorderLayout.CENTER);

        // --- BOTTOM PANEL (Status) ---
        infoLabel = new JLabel("Tentativo di connessione...", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        add(infoLabel, BorderLayout.SOUTH);

        setVisible(true);
        new Thread(this::connect).start();
    }

    private void connect() {
        try (Socket s = new Socket(serverIP, 15244)) {
            SwingUtilities.invokeLater(() -> infoLabel.setText("Connesso! In attesa del Banco..."));
            out = new PrintWriter(s.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            
            out.println("LOGIN:" + playerName);

            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.startsWith("BET_REQUEST:")) {
                    int amt = Integer.parseInt(msg.split(":")[1]);
                    SwingUtilities.invokeLater(() -> askBet(amt));
                } else if (msg.startsWith("WINNER_EVENT:")) {
                    String[] parts = msg.split(":");
                    String winnerName = parts[1];
                    double prize = Double.parseDouble(parts[2]);
                    SwingUtilities.invokeLater(() -> {
                        if (winnerName.contains(playerName)) {
                            myBalance += prize;
                            updateBalanceUI();
                            JOptionPane.showMessageDialog(this, "HAI VINTO IL BINGO! Premio: " + String.format("%.2f", prize) + "€");
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
                            JOptionPane.showMessageDialog(this, "Cinquina di " + parts[1]);
                        }
                    });
                } else if (msg.equals("RESET_GAME")) {
                    this.ticket = new BingoTicket(); // Crea una nuova cartella pulita
                    this.isPlaying = false;
                    SwingUtilities.invokeLater(() -> {
                        lastNumberLabel.setText("-");
                        betLabel.setText("Puntata: 0 €");
                        refreshTicketUI(); // Ridisegna la cartella pulita
                        infoLabel.setText("Nuova partita! In attesa della puntata...");
                    });
    } else if (msg.startsWith("POT_UPDATE:")) {
                    String pot = msg.split(":")[1];
                    SwingUtilities.invokeLater(() -> infoLabel.setText("Montepremi attuale: " + pot + " €"));
                } else {
                    try {
                        int n = Integer.parseInt(msg);
                        if(isPlaying) {
                            ticket.eliminateNumber(n);
                            SwingUtilities.invokeLater(() -> {
                                lastNumberLabel.setText(""+n);
                                refreshTicketUI();
                                if(ticket.checkBingo()) out.println("BINGO:"+playerName);
                                else if(ticket.checkCinquina()) out.println("CINQUINA:"+playerName);
                            });
                        }
                    } catch(Exception e) {}
                }
            }
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                infoLabel.setText("Disconnesso.");
                JOptionPane.showMessageDialog(this, "Errore connessione: " + e.getMessage());
            });
        }
    }

    private void askBet(int amt) {
        if (amt == 0) {
            // Partita gratis
            isPlaying = true;
            out.println("JOIN:" + playerName + ":0");
            betLabel.setText("Partita GRATIS");
            return;
        }
        
        int res = JOptionPane.showConfirmDialog(this, "Il Banco chiede una puntata di " + amt + "€. Accetti?", "Nuova Partita", JOptionPane.YES_NO_OPTION);
        if(res == JOptionPane.YES_OPTION && myBalance >= amt) {
            myBalance -= amt;
            isPlaying = true;
            out.println("JOIN:" + playerName + ":" + amt);
            betLabel.setText("Puntata: " + amt + " €");
            updateBalanceUI();
            infoLabel.setText("Sei in gioco! Buona fortuna.");
        } else {
            infoLabel.setText("Hai rifiutato la partita (Spettatore).");
        }
    }

    private void updateBalanceUI() {
        balanceLabel.setText("Saldo: " + String.format("%.2f", myBalance) + " €");
    }

private void refreshTicketUI() {
        for(int r=0; r<3; r++){
            for(int c=0; c<9; c++){
                int v = ticket.getSlotValue(r,c);
                JLabel l = gridLabels[r][c];
                if(v == 0) { 
                    l.setText(""); 
                    l.setBackground(Color.LIGHT_GRAY); 
                }
                else if(v == -1) { 
                    l.setText("X"); 
                    // CAMBIATO: La X ora ha lo sfondo VERDE
                    l.setBackground(new Color(46, 204, 113)); 
                    l.setForeground(Color.WHITE); 
                }
                else { 
                    l.setText(""+v); 
                    l.setBackground(Color.WHITE); 
                    l.setForeground(Color.BLACK); 
                }
            }
        }
    }
}