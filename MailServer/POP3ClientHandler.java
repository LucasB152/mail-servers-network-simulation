package MailServer;

import java.net.Socket;

public class POP3ClientHandler extends AbstractClientHandler {
    public POP3ClientHandler(Socket socket, String domain) {
        super(socket);
    }

    @Override
    public void analyzeMessage(String message) {
        //TODO : Implémenter la logique pour chaque message reçu
        //Utiliser la méthode super.sendMessage
    }
}
