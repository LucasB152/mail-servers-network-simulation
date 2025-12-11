import java.io.IOException;
import java.net.Socket;

public class IMAPClientHandler extends AbstractClientHandler {
    private boolean authenticated = false;
    private String currentUser = null;
    private MailStorage mailbox = null;

    public IMAPClientHandler(Socket socket, String domain) {
        super(socket, domain);
    }

    @Override
    protected void onConnect() {
        sendToClient("* OK IMAP server ready");
    }

    @Override
    public void analyzeMessage(String message) {
        String[] splits = message.split(" ", 3);
        String tag = splits[0];
        String command = splits[1];
        String arguments = null;
        if(splits.length > 2)
            arguments = splits[1];

        switch (command) {
            case "CAPABILITY":
                sendToClient("* CAPABILITY IMAP4rev1 UID");
                sendCompleted(tag, command);
                break;
            case "NOOP":
                sendCompleted(tag, command);
                break;
            case "LOGIN":
                handleLOGIN(tag, arguments);
                break;
        
            default:
                if(!authenticated) {
                    sendToClient(tag + " NO Authenticate first");
                    return;
                }
                break;
        }

        switch (command) {
            case "LIST":
                sendToClient("* LIST (\\HasNoChildren) \"/\" INBOX");
                sendCompleted(tag, command);
                //handleLIST(tag, arguments);
                break;
            case "SELECT":
                handleSELECT(tag, arguments);
                break;
            case "UID":
                handleUID(tag, arguments);
                break;
            case "LOGOUT":
                sendToClient("* BYE IMAP server logging out");
                sendCompleted(tag, command);
                break;
            default:
                sendToClient(tag + " BAD Unknown command");
                break;
        }
    }

    private void sendCompleted(String tag, String command) {
        String message = tag + " OK " + command + " completed";
        sendToClient(message);
    }

    private void handleLOGIN(String tag, String arguments) {
        if(arguments == null) {
            sendToClient(tag + " BAD Login missing arguments");
            return;
        }

        String[] splits = arguments.split(" ");
        if(splits.length < 2) {
            sendToClient(tag + " BAD Login missing arguments");
            return; 
        }
        String user = splits[0];
        String password = splits[1];

        if(!Users.userExists(user) || !Users.goodPassword(password)) {
            sendToClient(tag + " NO Invalid credentials");
            return;
        }

        authenticated = true;
        currentUser = user;
        mailbox = new MailStorage(currentUser);
        sendCompleted(tag, "LOGIN");
    }

    // Need to change it when using multiple directories
    //private void handleLIST(String tag, String arguments) {}

    private void handleSELECT(String tag, String arguments) {
        int count = mailbox.getMailsCount();
        sendToClient("* " + count + " EXISTS");
        sendToClient(tag + " OK [UIDVALIDITY " + mailbox.getUIDValidity() + "] SELECT completed");
    }

    private void handleUID(String tag, String arguments) {
        String[] splits = arguments.split(" ", 2);
        String subcommand = splits[0];

        if(subcommand.equals("FETCH")) 
            handleFETCH(tag, splits[1]);
        else if(subcommand.equals("STORE"))
            handleSTORE(tag, arguments);
        else
            sendToClient(tag + " BAD UID subcommand not supported");
    }

    private void handleFETCH(String tag, String arguments) {
        /*for(int i = 0; i < mailbox.getMailsCount(); i++) {
            try {
                int size = mailbox.readMail(i).getSize();

            }
            catch(IOException e) {e.printStackTrace();}
        }*/
    }

    private void handleSTORE(String tag, String arguments) {

    }
}
