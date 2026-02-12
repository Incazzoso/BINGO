import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class BingoPlayer {
    private BingoTicket ticket;
    private Socket session;

    public BingoPlayer() throws IOException {
        ticket = new BingoTicket();
        System.out.println("Cartella Iniziale:" + ticket);

        session = new Socket("localhost", 15244);
        BufferedReader in = new BufferedReader(new InputStreamReader(session.getInputStream()));

        // Ciclo di ascolto numeri dal server
        String line;
        while ((line = in.readLine()) != null) {
            if (line.equals("GAME_OVER")) {
                System.out.println("Fine della partita!");
                break;
            }

            int extracted = Integer.parseInt(line);
            System.out.println("\nNumero estratto dal Server: " + extracted);
            
            ticket.eliminateNumber(extracted); // Segna il numero se presente
            System.out.println(ticket);
        }
    }

    public static void main(String[] args) {
        try {
            new BingoPlayer();
        } catch (IOException e) {
            System.err.println("Impossibile connettersi al Server!");
        }
    }
}