import javax.swing.*;

public class BingoLauncher {
    public static void main(String[] args) {
        // Impostiamo il look and feel per renderlo più moderno
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}

        Object[] options = {"ENTRA IN PARTITA (Giocatore)", "CREA PARTITA (Server)"};
        
        int choice = JOptionPane.showOptionDialog(null, 
                "Benvenuto al Bingo! Cosa vuoi fare?", 
                "Bingo Launcher",
                JOptionPane.YES_NO_OPTION, 
                JOptionPane.QUESTION_MESSAGE, 
                null, 
                options, 
                options[0]); // Default su "Giocatore"

        if (choice == 0) {
            // Scelta: Giocatore
            String ip = JOptionPane.showInputDialog(null, "Inserisci IP del Server (es. 192.168.1.5):", "127.0.0.1");
            if (ip != null && !ip.isEmpty()) {
                new BingoPlayerGUI(ip);
            }
        } else if (choice == 1) {
            // Scelta: Server
            new BingoHallGUI();
        } else {
            System.exit(0);
        }
    }
}