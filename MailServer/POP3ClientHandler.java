import java.io.IOException;
import java.net.Socket;
import java.util.*;

public class POP3ClientHandler extends AbstractClientHandler {
    private POP3State state = POP3State.AUTH;    
    private String currentUser = null;
    private final List<Integer> mailsToDelete = new ArrayList<>();
    private MailStorage mailbox = null;

    public POP3ClientHandler(Socket socket, String domain) {
        super(socket, domain);
    }

    @Override
    protected void onConnect() {
        sendOK("POP3 server ready");
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
        if(!Users.goodPassword(password)) {
            sendERR("Invalid password");
            return;
        }

        mailbox = new MailStorage(currentUser);
        state = POP3State.TRANSACTION;
        sendOK("Mailbox locked and ready");
    }

    private void handleSTAT() {
        if(state != POP3State.TRANSACTION) {
            sendNotAuthorized();
            return;
        }
        
        int countMails = mailbox.getMailsCount();
        System.out.println("Nbr mails = " + countMails);
        int sizeMails = 0;
        for(int i = 1; i <= countMails; i++) {
            try {
                Email email = mailbox.getMail(i);
                sizeMails += email.getSize();
            }
            catch(IOException e) {e.printStackTrace();}
        }

        sendOK(countMails + " " + sizeMails);
    }

    private void handleLIST() {
        if(state != POP3State.TRANSACTION) {
            sendNotAuthorized();
            return;
        }

        int countMails = mailbox.getMailsCount();
        int sizeMails = 0;
        for(int i = 1; i <= countMails; i++) {
            try {
                Email email = mailbox.getMail(i);
                sizeMails += email.getSize();
            }
            catch(IOException e) {e.printStackTrace();}
        }
        sendOK(countMails + "messages (" + sizeMails + " octets)");

        for(int i = 1; i <= countMails; i++) {
            try {
                Email email = mailbox.getMail(i);
                sendToClient(i + " " + email.getSize());
            }
            catch(IOException e) {sendERR("Unable to read message");}
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
            try {
                Email email = mailbox.getMail(id);
                if(email == null) {
                    sendERR("No such message");
                    return;
                }

                sendOK(email.getSize() + " octets");
                sendToClient("From: " + email.getFrom());
                sendToClient("To: " + email.getTo());
                sendToClient("Subject: " + email.getSubject());
                sendToClient("");
                sendToClient(email.getBody());
                sendToClient(".");
            }
            catch(IOException e) {sendERR("Could not read message");}
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
            if(id < 1 || id > mailbox.getMailsCount()) {
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
            for(int index : mailsToDelete)
                mailbox.deleteMail(index);
        }

        state = POP3State.UPDATE;
        sendOK("Goodbye");
    }
}
