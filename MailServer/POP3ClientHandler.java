package MailServer;

import java.net.Socket;
import java.util.*;

public class POP3ClientHandler extends AbstractClientHandler {
    private POP3State state = POP3State.AUTH;
    
    private String currentUser = null;

    // Temporary messages storage
    private static final Map<String, List<Email>> mailbox = new HashMap<>();
    private final List<Integer> mailsToDelete = new ArrayList<>();

    public POP3ClientHandler(Socket socket, String domain) {
        super(socket, domain);
    }

    @Override
    public void run() {
        sendToClient("+OK POP3 server ready");
        super.run();
    }

    @Override
    public void analyzeMessage(String message) {
        String[] splits = message.split(" ", 2);
        String command = splits[0];
        String argument = null;
        if(splits.length > 1)
            argument = splits[1];

        switch (command) {
            case "USER":
                handleUSER(argument);
                break;
            case "PASS":
                handlePASS(argument);
                break;
            case "STAT":
                handleSTAT();
                break;
            case "LIST":
                handleLIST();
                break;
            case "RETR":
                handleRETR(argument);
                break;
            case "DELE":
                handleDELE(argument);
                break;
            case "QUIT":
                handleQUIT();
                break;
            default:
                sendToClient("-ERR Unknown command");
                break;
        }
    }

    private void sendOK(String message) {
        String result = "+OK ";
        result += message;
        sendToClient(result);
    }
    
    private void sendERR(String message) {
        String result = "-ERR ";
        result += message;
        sendToClient(result);
    }

    private void sendNotAuthorized() {
        sendToClient("-ERR Not authorized");
    }

    private void handleUSER(String username) {
        if(state != POP3State.AUTH) {
            sendERR("Already authenticated");
            return;
        }
        if(username == null || !username.contains("@")) {
            sendERR("Invalid username");
        }

        if(Users.userExists(username)) {
            currentUser = username;
            sendOK("");
        }
        else
            sendERR("Username not existing");
    }

    private void handlePASS(String password) {
        if(state != POP3State.AUTH) {
            sendERR("Already authenticated");
            return;
        }
        if(currentUser == null) {
            sendERR("USER required for PASS");
            return;
        }
        if(!"password".equals(password)) {
            sendERR("Invalid password");
            return;
        }

        state = POP3State.TRANSACTION;
        sendOK("Mailbox locked and ready");
    }

    private void handleSTAT() {
        if(state != POP3State.TRANSACTION) {
            sendNotAuthorized();
            return;
        }

        // TODO: besoin de changer quand le vrai systeme de fichier est là
        List<Email> listMails = mailbox.get(currentUser);
        int size = 0;
        for(Email mail : listMails) {
            size += mail.getSize();
        }

        sendOK(listMails.size() + " " + size);
    }

    private void handleLIST() {
        if(state != POP3State.TRANSACTION) {
            sendNotAuthorized();
            return;
        }

        List<Email> listMails = mailbox.get(currentUser);
        int size = 0;
        for(Email mail : listMails) {
            size += mail.getSize();
        }
        sendOK(listMails.size() + "messages (" + size + " octets)");

        for(int i = 0; i < listMails.size(); i++) {
            sendToClient((i + 1) + " " + listMails.get(i).getSize());
        }

        sendToClient(".");
    }

    private void handleRETR(String argument) {
        if(state != POP3State.TRANSACTION) {
            sendNotAuthorized();
            return;
        }
        if(argument == null) {
            sendERR("Missing index");
            return;
        }

        try {
            int id = Integer.parseInt(argument);
            List<Email> listMails = mailbox.get(currentUser);

            if(id < 1 || id > listMails.size()) {
                sendERR("No such message");
                return;
            }

            Email email = listMails.get(id - 1);

            sendOK(email.getSize() + " octets");
            sendToClient("From: " + email.getFrom());
            sendToClient("To: " + email.getTo());
            sendToClient("Subject: " + email.getSubject());
            sendToClient("");
            sendToClient(email.getBody());
            sendToClient(".");
        }
        catch(NumberFormatException e) {sendERR("Invalid argument");}
    }

    private void handleDELE(String argument) {
        if(state != POP3State.TRANSACTION) {
            sendNotAuthorized();
            return;
        }
        if(argument == null) {
            sendERR("Missing index");
            return;
        }

        try {
            int id = Integer.parseInt(argument);
            List<Email> listMails = mailbox.get(currentUser);

            if(id < 1 || id > listMails.size()) {
                sendERR("No such message");
                return;
            }

            mailsToDelete.add(id - 1);
            sendOK("Message marked for deletion");
        }
        catch(NumberFormatException e) {sendERR("Invalid argument");}
    }

    private void handleQUIT() {
        if(state == POP3State.TRANSACTION) {
            List<Email> listMails = mailbox.get(currentUser);
            for(int index : mailsToDelete)
                listMails.remove(index);
        }

        state = POP3State.UPDATE;
        sendOK("Goodbye");
    }
}
