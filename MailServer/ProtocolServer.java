import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Generic server responsible for listening on a given port and dispatching
 * client connections to the appropriate protocol handler.
 *
 * @author Lucas BAUDUIN & Lucas GIOT
 */
public class ProtocolServer {
    private final ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private final int portNumber;
    private final String domain;

    private Map<String, String> knownServers = new HashMap<>();

    /**
     * Creates and starts a protocol server without predefined forwarding rules.
     *
     * @param portNumber  port on which the server listens
     * @param threadPool  shared thread pool used to handle client connections
     * @param domain      domain name of the mail server
     */
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

    /**
     * Creates and starts a protocol server with predefined known mail servers
     * used for SMTP forwarding.
     *
     * @param threadPool    shared thread pool used to handle client connections
     * @param portNumber    port on which the server listens
     * @param domain        domain name of the mail server
     * @param knownServers  map of known domains to their mail server IP addresses
     */
    public ProtocolServer(ExecutorService threadPool, int portNumber, String domain, Map<String, String> knownServers) {
        try {
            this.serverSocket = new ServerSocket(portNumber);
        } catch (IOException e) {
            throw new RuntimeException("Error while creating the server. We cannot run this program.");
        }
        this.portNumber = portNumber;
        this.threadPool = threadPool;
        this.domain =domain;
        this.knownServers = knownServers;

        startServer();
    }

    /**
     * Continuously listens for incoming client connections and assigns
     * each connection to the thread pool.
     */
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

    /**
     * Creates the appropriate client handler based on the listening port.
     *
     * @param client the socket associated with the connected client
     * @return an AbstractClientHandler matching the protocol
     */
    private AbstractClientHandler createHandler(Socket client) {
        return switch (this.portNumber) {
            case 25 -> new SMTPClientHandler(client, domain, knownServers);
            case 110 -> new POP3ClientHandler(client, domain);
            case 143 -> new IMAPClientHandler(client, domain);
            default -> throw new IllegalArgumentException("Unknown port: " + this.portNumber);
        };
    }

    /**
     * Stops the server by closing the server socket and shutting down the thread pool.
     *
     * @throws IOException if an error occurs while closing the server socket
     */
    public void stopServer() throws IOException {
        serverSocket.close();
        threadPool.shutdown();
    }
}
