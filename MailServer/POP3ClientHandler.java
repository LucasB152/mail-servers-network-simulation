import java.io.IOException;
import java.net.Socket;
import java.util.*;

/**
 * Handles a POP3 client session.
 * This class manages authentication, message retrieval, and deletion
 * according to the POP3 protocol.
 *
 * @author Lucas BAUDUIN & Lucas GIOT
 */
public class POP3ClientHandler extends AbstractClientHandler {
    private POP3State state = POP3State.AUTH;    
    private String currentUser = null;
    private final List<Integer> mailsToDelete = new ArrayList<>();
    private MailStorage mailbox = null;

    /**
     * Creates a new POP3 client handler.
     *
     * @param socket client socket
     * @param domain domain name of the mail server
     */
    public POP3ClientHandler(Socket socket, String domain) {
        super(socket, domain);
    }

    /**
     * Sends the POP3 greeting when a client connects.
     */
    @Override
    protected void onConnect() {
        sendOK("POP3 server ready");
    }

    /**
     * Parses and dispatches POP3 commands received from the client.
     *
     * @param message raw POP3 command
     */
    @Override
    public void analyzeMessage(String message) {
        String[] splits = message.split(" ", 2);
        String command = splits[0];
        String argument = null;
        if(splits.length > 1)
            argument = splits[1];

        switch (command) {
            case "CAPA":
                sendOK("Capability list follows");
                sendToClient(".");
                break;
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
                sendERR("Unknown command");
                break;
        }
    }

    /**
     * Sends a positive POP3 response.
     *
     * @param message response message
     */
    private void sendOK(String message) {
        String result = "+OK ";
        result += message;
        sendToClient(result);
    }

    /**
     * Sends a negative POP3 response.
     *
     * @param message error message
     */
    private void sendERR(String message) {
        String result = "-ERR ";
        result += message;
        sendToClient(result);
    }

    /**
     * Sends a not-authorized error message.
     */
    private void sendNotAuthorized() {
        sendToClient("-ERR Not authorized");
    }

    /**
     * Handles the USER command.
     *
     * @param username username provided by the client
     */
    private void handleUSER(String username) {
        if(state != POP3State.AUTH) {
            sendERR("Already authenticated");
            return;
        }
        if(username == null || !username.contains("@")) {
            sendERR("Invalid username");
        }

        String[] splits = username.split("@", 2);
        String domain = splits[1];

        if(Users.userExists(username) && domain.equals(super.domain)) {
            currentUser = username;
            sendOK("");
        }
        else
            sendERR("Username not existing");
    }

    /**
     * Handles the PASS command and authenticates the user.
     *
     * @param password password provided by the client
     */
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

    /**
     * Handles the STAT command.
     */
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

    /**
     * Handles the LIST command.
     */
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
        sendOK(countMails + " messages (" + sizeMails + " octets)");

        for(int i = 1; i <= countMails; i++) {
            try {
                Email email = mailbox.getMail(i);
                sendToClient(i + " " + email.getSize());
            }
            catch(IOException e) {sendERR("Unable to read message");}
        }

        sendToClient(".");
    }

    /**
     * Handles the RETR command and sends the message content.
     *
     * @param argument message index
     */
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

    /**
     * Handles the DELE command and marks a message for deletion.
     *
     * @param argument message index
     */
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

            mailsToDelete.add(id);
            sendOK("Message marked for deletion");
        }
        catch(NumberFormatException e) {sendERR("Invalid argument");}
    }

    /**
     * Handles the QUIT command and applies pending deletions.
     */
    private void handleQUIT() {
        if(state == POP3State.TRANSACTION) {
            for(int index : mailsToDelete)
                mailbox.deleteMail(index);
        }

        state = POP3State.UPDATE;
        sendOK("Goodbye");
    }
}
