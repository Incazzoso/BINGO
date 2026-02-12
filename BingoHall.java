import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BingoHall {
    private final List<PrintWriter> players = Collections.synchronizedList(new ArrayList<>());
    private final BingoNumberGenerator generator = new BingoNumberGenerator();

    public BingoHall() throws IOException {
        ServerSocket serverSocket = new ServerSocket(15244);
        System.out.println("Bingo Hall avviata. In attesa di giocatori...");

        // Thread per accettare connessioni continue
        new Thread(() -> {
            while (true) {
                try {
                    Socket client = serverSocket.accept();
                    PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                    players.add(out);
                    System.out.println("Giocatore connesso! Totale: " + players.size());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        runGame();
    }

    private void runGame() {
        while (true) {
            try {
                Thread.sleep(3000); // Pausa tra le estrazioni
                int num = generator.nextInt();
                
                if (num == 0) {
                    broadcast("GAME_OVER");
                    break;
                }

                System.out.println("Estratto: " + num);
                broadcast(String.valueOf(num));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void broadcast(String msg) {
        synchronized (players) {
            for (PrintWriter p : players) p.println(msg);
        }
    }

    public static void main(String[] args) throws IOException {
        new BingoHall();
    }
}