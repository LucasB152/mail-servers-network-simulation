import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public class ProtocolServer {
    private final ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private final int portNumber;
    private final String domain;

    public ProtocolServer(int portNumber, ExecutorService threadPool, String domain) {
        try {
            this.serverSocket = new ServerSocket(portNumber);
        } catch (IOException e) {
            throw new RuntimeException("Error while creating the server. We cannot run this program.");
        }
        this.portNumber = portNumber;
        this.threadPool = threadPool;
        this.domain =domain;

        startServer();
    }

    public void startServer() {
        while (true) {
            try {
                Socket clientSocket = this.serverSocket.accept();
                threadPool.execute(createHandler(clientSocket));
            } catch (IOException e) {
                System.out.println("Error while listening for new connection on the server : " + e.getMessage());
            }
        }
    }

    private AbstractClientHandler createHandler(Socket client) {
        return switch (this.portNumber) {
            case 25 -> new SMTPClientHandler(client, domain);
            case 110 -> new POP3ClientHandler(client, domain);
            case 143 -> new IMAPClientHandler(client, domain);
            default -> throw new IllegalArgumentException("Unknown port: " + this.portNumber);
        };
    }

    public void stopServer() throws IOException {
        serverSocket.close();
        threadPool.shutdown();
    }

}
