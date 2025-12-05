package MailServer;

import java.net.Socket;

public class IMAPClientHandler extends AbstractClientHandler {

    public IMAPClientHandler(Socket socket) {
        super(socket);
    }

    @Override
    public void analyzeMessage(String message) {
        //TODO : Implémenter la logique pour chaque message reçu
        //Utiliser la méthode super.sendMessage
    }
}
