import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public class ProtocolServer {
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private int portNumber;
    private String domain;

    public ProtocolServer(int portNumber, ExecutorService threadPool, String domain) {
        try {
            this.serverSocket = new ServerSocket(portNumber);
        } catch (IOException e) {
            throw new RuntimeException("Error while creating the server. We cannot run this program.");
        }
        this.portNumber = portNumber;
        this.domain = domain;
        this.threadPool = threadPool;

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
        switch (this.portNumber) {
            case 25:
                return new SMTPClientHandler(client);
            case 110:
                return new POP3ClientHandler(client);
            case 143:
                return new IMAPClientHandler(client);
            default:
                throw new IllegalArgumentException("Unknown port: " + this.portNumber);
        }
    }

    public void stopServer() throws IOException {
        serverSocket.close();
        threadPool.shutdown();
    }

}
