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
        String command = splits[1].toUpperCase();
        String arguments = null;
        if(splits.length > 2)
            arguments = splits[2];

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
            case "CAPABILITY":
                break;
            case "NOOP":
                break;
            case "LOGIN":
                break;
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
        if(user.startsWith("\""))
            user = user.substring(1, user.length() - 1);
        String password = splits[1];
        if(password.startsWith("\""))
            password = password.substring(1, password.length() - 1);
        System.out.println(user + " " + password);

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
        int dataStart = arguments.indexOf('(');
        if(dataStart == -1) {
            sendToClient(tag + " BAD Invalid FETCH syntax");
            return;
        }

        String range = arguments.substring(0, dataStart).trim();
        String dataItems = arguments.substring(dataStart);
        String[] splits = range.split(":", 2);
        int maxRange = 1;
        try {
            maxRange = Integer.parseInt(splits[1]);
        }
        catch(NumberFormatException e) {sendToClient(tag + " BAD Invalid range");}


        for(int i = 1; i < maxRange; i++) {
            try {
                Email email = mailbox.getMail(i);
                int size = email.getSize();
                String body = email.getBody();

                /*sendToClient("* " + (i+1) + " FETCH (UID " + m.uid +
                    " FLAGS (" + String.join(" ", m.flags) + ") " +
                    "RFC822.SIZE " + size +
                    " BODY[] {" + size + "}");
                sendToClient(body);
                sendToClient(")");*/
            }
            catch(IOException e) {e.printStackTrace();}
        }

        sendCompleted(tag, "FETCH");
    }

    private void handleSTORE(String tag, String arguments) {

    }
}
