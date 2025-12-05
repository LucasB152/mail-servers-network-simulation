package MailServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public abstract class AbstractClientHandler implements Runnable {
    private final Socket socket;
    private BufferedReader inputReader;
    private PrintWriter outputWriter;

    public AbstractClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            this.inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.outputWriter = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = inputReader.readLine()) != null) {
                System.out.println("Message reçu : " + line);
                analyzeMessage(line);
            }

        } catch (IOException e) {
            System.out.println("Error while manipulating stream : " + e.getMessage());
        }
    }

    protected void sendToClient(String message){
        this.outputWriter.print(message + "\r\n");
        this.outputWriter.flush();
    }

    abstract public void analyzeMessage(String message);
}
