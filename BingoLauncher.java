import javax.swing.*;
import java.net.InetAddress;

public class BingoLauncher {
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}

        Object[] options = {"CREA PARTITA (Server/Banco)", "ENTRA IN PARTITA (Giocatore)"};
        
        int choice = JOptionPane.showOptionDialog(null, 
                "BINGO GOLD EDITION\nCosa vuoi fare?", 
                "Bingo Launcher",
                JOptionPane.YES_NO_OPTION, 
                JOptionPane.QUESTION_MESSAGE, 
                null, 
                options, 
                options[0]);

        if (choice == 0) {
            // Avvia il Server (UI Ricca)
            new BingoHallGUI();
        } else if (choice == 1) {
            // Trova IP locale per suggerimento
            String defaultIP = "";
            try { defaultIP = InetAddress.getLocalHost().getHostAddress(); } catch(Exception e){}
            
            String ip = JOptionPane.showInputDialog(null, "Inserisci IP del Server:", defaultIP);
            if (ip != null && !ip.trim().isEmpty()) {
                new BingoPlayerGUI(ip.trim());
            } else {
                System.exit(0);
            }
        } else {
            System.exit(0);
        }
    }
}