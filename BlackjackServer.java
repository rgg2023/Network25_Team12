import java.net.ServerSocket;
import java.net.Socket;

public class BlackjackServer {
    public static void main(String[] args) {
        int port = 6789;
        
        // 1. Create Shared Resource (GameRoom)
        // All handler threads share this single object.
        GameRoom gameRoom = new GameRoom();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Blackjack Server started on port " + port);

            while (true) {
                // 2. Wait for client connections
                Socket clientSocket = serverSocket.accept();
                System.out.println("New player connected: " + clientSocket.getInetAddress());

                // 3. Create and start handler thread (Multi-thread)
                ClientHandlerB handler = new ClientHandlerB(clientSocket, gameRoom);
                new Thread(handler).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
