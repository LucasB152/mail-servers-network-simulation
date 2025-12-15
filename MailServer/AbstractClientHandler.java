import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public abstract class AbstractClientHandler implements Runnable {
    private final Socket socket;
    protected final String domain;
    private BufferedReader inputReader;
    private PrintWriter outputWriter;

    private Boolean isThreadRunning = true;

    public AbstractClientHandler(Socket socket, String domain) {
        this.socket = socket;
        this.domain = domain;
    }

    @Override
    public void run() {
        try {
            this.inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.outputWriter = new PrintWriter(socket.getOutputStream(), true);

            onConnect();

            String line;
            while (isThreadRunning && (line = inputReader.readLine()) != null) {
                System.out.println("Message reçu : " + line);
                analyzeMessage(line);
            }

        } catch (IOException e) {
            System.out.println("Error while manipulating stream : " + e.getMessage());
        }
    }
    
    protected void onConnect() {
        // If we need to do things before listening to client
        // Override in SMTP, POP3, IMAP
    }

    protected void sendToClient(String message){
        this.outputWriter.println(message);
        System.out.println(message);
    }

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

    abstract public void analyzeMessage(String message);
}
