import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandlerB implements Runnable {
    private Socket socket;
    private GameRoom gameRoom;
    private PrintWriter out;
    private BufferedReader in;

    // Player Information
    private String playerId;
    private int balance = 1000; // Initial Balance
    private int currentBet = 0;
    private int score = 0;

    private boolean isBetPlaced = false; // Check if bet is placed
    private boolean isSurrender = false; // Check if surrendered

    public ClientHandlerB(Socket socket, GameRoom room) {
        this.socket = socket;
        this.gameRoom = room;
        // Generate temporary ID (Login protocol required in real app)
        this.playerId = "Player" + (int)(Math.random() * 1000);
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Join Game Room
            gameRoom.join(this);
            sendMessage("WELCOME: Welcome to the Blackjack Server! (ID: " + playerId + ", Balance: " + balance + ")");

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                // Parse Protocol (Format: "COMMAND:DATA")
                String[] parts = inputLine.split(":", 2);
                String command = parts[0];
                String data = parts.length > 1 ? parts[1] : "";

                if (command.equals("START")) { // ★ Game Start Command
                    gameRoom.startGame();
                
                } else if (command.equals("PLACE_BET")) {
                    try {
                        int amount = Integer.parseInt(data);
                        gameRoom.placeBet(this, amount);
                    } catch (NumberFormatException e) {
                        sendMessage("ERROR: Invalid bet amount.");
                    }
                } else if (command.equals("PLAYER_ACTION")) {
                    gameRoom.handlePlayerAction(this, data);
                } else if (command.equals("BALANCE")) { // Added for completeness
                    sendMessage("INFO: Current Balance is [" + balance + "].");
                }
            }
        } catch (Exception e) {
            System.out.println("Connection Closed: " + playerId);
        } finally {
            gameRoom.leave(this);
            try { socket.close(); } catch (Exception e) {}
        }
    }

    public void sendMessage(String msg) {
        if (out != null) out.println(msg);
    }

    public void resetRound() {
        this.score = 0;
        this.currentBet = 0;
        this.isBetPlaced = false; // Reset
        this.isSurrender = false; // Reset
    }

    public boolean isBetPlaced() { return isBetPlaced; }
    public void setBetPlaced(boolean placed) { this.isBetPlaced = placed; }
    
    public boolean isSurrender() { return isSurrender; }
    public void setSurrender(boolean surrender) { this.isSurrender = surrender; }

    // Getter/Setter
    public String getPlayerId() { return playerId; }
    public int getBalance() { return balance; }
    public void decreaseBalance(int amount) { this.balance -= amount; }
    public int getCurrentBet() { return currentBet; }
    public void setCurrentBet(int amount) { this.currentBet = amount; }
    public void addCardScore(int score) { this.score += score; }
    public int getScore() { return score; }
    public void resetScoreOnly() { this.score = 0; }
    public void increaseBalance(int amount) { this.balance += amount; }
}
