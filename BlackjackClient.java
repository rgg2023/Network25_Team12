import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class BlackjackClient {
    public static void main(String[] args) {
        // Should read from serverinfo.dat, but hardcoded for convenience
        String serverIP = "127.0.0.1";
        int serverPort = 6789;

        try (Socket socket = new Socket(serverIP, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to the server.");
            
            // 1. Thread for receiving server messages (Listener)
            Thread listener = new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        System.out.println("[Server] " + msg);
                    }
                } catch (Exception e) {
                    System.out.println("Connection to server lost.");
                }
            });
            listener.start();

            // 2. Send user input (Main Thread)
            System.out.println("Command Guide:");
            System.out.println(" - Start Game: START");
            System.out.println(" - Place Bet: BET <Amount>  (e.g., BET 100)");
            System.out.println(" - Actions: HIT, STAND, DOUBLEDOWN, SURRENDER");
            
            while (true) {
                String input = scanner.nextLine();
                
                if (input.startsWith("BET ")) {
                    // Convert protocol: "BET 100" -> "PLACE_BET:100"
                    String amount = input.split(" ")[1];
                    out.println("PLACE_BET:" + amount);
                } else if (input.equalsIgnoreCase("HIT")) {
                    // "HIT" -> "PLAYER_ACTION:Hit"
                    out.println("PLAYER_ACTION:Hit");
                } else if (input.equalsIgnoreCase("STAND")) {
                    // "STAND" -> "PLAYER_ACTION:Stand"
                    out.println("PLAYER_ACTION:Stand");
                } else if (input.equalsIgnoreCase("START")){
                    // "START" -> "Game START"
                    out.println("START");
                } else if (input.equalsIgnoreCase("DOUBLEDOWN")) { 
                    // "DOUBLEDOWN" -> "PLAYER_ACTION:DoubleDown"
                    out.println("PLAYER_ACTION:DoubleDown");
                } else if (input.equalsIgnoreCase("SURRENDER")) { 
                    // "SURRENDER" -> "PLAYER_ACTION:Surrender"
                    out.println("PLAYER_ACTION:Surrender");
                } else {
                    System.out.println("Unknown command. Please check spelling.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
