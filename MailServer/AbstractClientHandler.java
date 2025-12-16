import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Abstract base class for handling a client connection.
 * It defines the common behavior shared by all protocol handlers
 * (SMTP, POP3, and IMAP).
 *
 * @author Lucas BAUDUIN & Lucas GIOT
 */
public abstract class AbstractClientHandler implements Runnable {
    private final Socket socket;
    protected final String domain;
    private BufferedReader inputReader;
    private PrintWriter outputWriter;

    private Boolean isThreadRunning = true;

    /**
     * Creates a new client handler for a given socket and domain.
     *
     * @param socket client socket associated with the connection
     * @param domain domain name of the mail server
     */
    public AbstractClientHandler(Socket socket, String domain) {
        this.socket = socket;
        this.domain = domain;
    }

    /**
     * Main execution method of the handler.
     * It initializes the input and output streams, calls the connection hook, and processes incoming client messages.
     */
    @Override
    public void run() {
        try {
            this.inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.outputWriter = new PrintWriter(socket.getOutputStream(), true);

            onConnect();

            String line;
            while (isThreadRunning && (line = inputReader.readLine()) != null) {
                System.out.println("[" + this.getClass() + "] Message reçu : " + line);
                analyzeMessage(line);
            }

        } catch (IOException e) {
            System.out.println("Error while manipulating stream : " + e.getMessage());
        }
    }

    /**
     * Sends a message to the connected client.
     * @param message message to send
     */
    protected void sendToClient(String message){
        this.outputWriter.println(message);
        System.out.println("[" + this.getClass() + "] Message envoyé : " + message);
    }

    /**
     * Closes the client connection and stops the handler execution.
     */
    protected void closeConnection(){
        isThreadRunning = false;
        try {
            this.inputReader.close();
            this.outputWriter.close();
            this.socket.close();
        } catch (IOException e) {
            System.err.println("Impossible to close the socket");
        }
    }

    /**
     * Analyzes and processes a message received from the client.
     * Must be implemented by protocol-specific handlers.
     *
     * @param message message received from the client
     */
    abstract public void analyzeMessage(String message);

    /**
     * Hook method called when a client connects.
     * Must be implemented by protocol-specific handlers.
     */
    abstract protected void onConnect();
}
