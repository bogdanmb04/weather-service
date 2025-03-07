package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private final int PORT = 6543;
    public void start() {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            Socket client = new Socket();

            System.out.println("Server is running...");
            while(true) {
                Socket acceptedClient = serverSocket.accept();
                new ClientThread(acceptedClient).start();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
