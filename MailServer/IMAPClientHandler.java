import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
                handleLIST(tag, arguments);
                break;
            case "SELECT":
                handleSELECT(tag, arguments);
                break;
            case "CREATE":
                handleCREATE(tag, arguments);
                break;
            case "DELETE":
                handleDELETE(tag, arguments);
                break;
            case "RENAME":
                handleRENAME(tag, arguments);
                break;
            case "APPEND":
                sendNO(tag, "APPEND not supported");
                break;
            case "CHECK":
                sendCompleted(tag, "CHECK");
                break;
            case "UID":
                handleUID(tag, arguments);
                break;
            case "LOGOUT":
                sendToClient("* BYE IMAP server logging out");
                sendCompleted(tag, command);
                break;
            case "EXPUNGE":
                handleEXPUNGE(tag);
                break;
            case "CLOSE":
                handleCLOSE(tag);
                break;
            
            default:
                sendNO(tag, "Command not implemented");
                break;
        }
    }

    private void sendCompleted(String tag, String command) {
        String response = tag + " OK " + command + " completed";
        sendToClient(response);
    }
    
    private void sendNO(String tag, String message) {
        String response = tag + " NO " + message;
        sendToClient(response);
    }

    private void sendBAD(String tag, String message) {
        String response = tag + " BAD " + message;
        sendToClient(response);
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

        if(!Users.userExists(user) || !Users.goodPassword(password)) {
            sendToClient(tag + " NO Invalid credentials");
            return;
        }

        authenticated = true;
        currentUser = user;
        mailbox = new MailStorage(currentUser);
        sendCompleted(tag, "LOGIN");
    }

    private void handleLIST(String tag, String arguments) {
        List<String> mailboxes = mailbox.listMailBoxes();

        for(String mailbox : mailboxes)
            sendToClient("* LIST (\\HasNoChildren) \"/\" " + mailbox);

        sendCompleted(tag, "LIST");
    }

    private void handleSELECT(String tag, String arguments) {
        String mailboxName = arguments.trim();
        if(mailboxName.startsWith("\""))
            mailboxName = mailboxName.substring(1, mailboxName.length() - 1);

        if(mailboxName.isEmpty()) {
            sendBAD(tag, "No mailbox name specified");
            return;
        }
        if (!mailbox.mailboxExists(mailboxName)) {
            sendNO(tag, "Mailbox does not exist");
            return;
        }

        mailbox.selectMailbox(mailboxName);

        int count = mailbox.getMailsCount();
        sendToClient("* " + count + " EXISTS");
        sendToClient(tag + " OK [UIDVALIDITY " + mailbox.getUIDValidity() + "] SELECT completed");
    }

    private void handleCREATE(String tag, String arguments) {
        String mailboxName = arguments.trim();
        if(mailboxName.isEmpty()) {
            sendBAD(tag, "No mailbox name specified");
            return;
        }
        if(mailboxName.startsWith("\""))
            mailboxName = mailboxName.substring(1, mailboxName.length() - 1);
    
        if(mailboxName.equalsIgnoreCase("INBOX")) {
            sendNO(tag, "Cannot create INBOX");
            return;
        }
        if(mailbox.mailboxExists(mailboxName)) {
            sendNO(tag, "Mailbox already exists");
            return;
        }

        boolean created = mailbox.createMailbox(mailboxName);
        if(created) 
            sendCompleted(tag, "CREATE");
        else
            sendNO(tag, "CREATE failed");
    }

    private void handleDELETE(String tag, String arguments) {
        String mailboxName = arguments.trim();
        if(mailboxName.isEmpty()) {
            sendBAD(tag, "No mailbox name specified");
            return;
        }
        if(mailboxName.startsWith("\""))
            mailboxName = mailboxName.substring(1, mailboxName.length() - 1);

        if(mailboxName.equalsIgnoreCase("INBOX")) {
            sendNO(tag, "Cannot delete INBOX");
            return;
        }

        if(mailboxName.startsWith("Trash/"))
            mailboxName = mailboxName.substring("Trash/".length());
        if(!mailbox.mailboxExists(mailboxName)) {
            sendNO(tag, "Mailbox already deleted");
            return;
        }

        boolean deleted = mailbox.deleteMailbox(mailboxName);
        if(deleted) {
            if(mailbox.getCurrentMailbox().equalsIgnoreCase(mailboxName))
                mailbox = null;
            sendCompleted(tag, "DELETE");
        }
        else
            sendNO(tag, "DELETE failed");
    }

    private void handleRENAME(String tag, String arguments) {
        String[] splits = arguments.split(" ", 2);
        if(splits.length < 2) {
            sendBAD(tag, "Invalid RENAME syntax");
            return;
        }

        String oldName = splits[0];
        String newName = splits[1];

        if (oldName.equalsIgnoreCase("INBOX")) {
            sendNO(tag, "Cannot rename INBOX");
            return;
        }

        if(!mailbox.mailboxExists(oldName)) {
            sendNO(tag, "Source mailbox does not exists");
            return;
        }
        if(mailbox.mailboxExists(newName)) {
            sendNO(tag, "Destination mailbox already exists");
            return;
        }

        boolean renamed = mailbox.renameMailbox(oldName, newName);
        if(renamed) {
            if(mailbox.getCurrentMailbox().equals(oldName))
                mailbox.selectMailbox(newName);
            sendCompleted(tag, "RENAME");
        }
        else
            sendNO(tag, "RENAME failed");
    }

    private void handleUID(String tag, String arguments) {
        String[] splits = arguments.split(" ", 2);
        String subcommand = splits[0].toUpperCase();

        if(subcommand.equals("FETCH")) 
            handleFETCH(tag, splits[1]);
        else if(subcommand.equals("STORE"))
            handleSTORE(tag, arguments);
        else
            sendToClient(tag + " BAD UID subcommand not supported");
    }

    private List<Integer> computeRange(String tag, String range) {
        List<Integer> realRange = new ArrayList<>();

        if(range.equals("*")) {
            int nbrMails = mailbox.getMailsCount();
            for(int i = 1; i <= nbrMails; i++)
                realRange.add(i);
            return realRange;
        }
        else if(range.contains(":")) {
            String[] splits = range.split(":", 2);
            if(splits[1].equals("*")) {
                int nbrMails = mailbox.getMailsCount();
                int start = Integer.parseInt(splits[0]);
                for(; start <= nbrMails; start++)
                    realRange.add(start);
                return realRange;
            }
            else {
                int maxRange = Integer.parseInt(splits[1]);
                for(int i = 1; i <= maxRange; i++)
                    realRange.add(i);
            }
        }
        else if(range.contains(",")) {
            String[] splits = range.split(",");
            for (String uid : splits) {
                realRange.add(Integer.parseInt(uid.trim()));
            }
        }
        else
            realRange.add(Integer.parseInt(range));

        return realRange;
    }

    private void handleFETCH(String tag, String arguments) {
        int dataStart = arguments.indexOf('(');
        if(dataStart == -1) {
            sendToClient(tag + " BAD Invalid FETCH syntax");
            return;
        }

        String range = arguments.substring(0, dataStart).trim();
        String dataItems = arguments.substring(dataStart);
        List<Integer> realRange = computeRange(tag, range);

        for(int i : realRange) {
            try {
                Email email = mailbox.getMail(i);
                int size = email.printIMAP().getBytes(StandardCharsets.UTF_8).length;
                StringBuilder response = new StringBuilder();

                response.append("* ").append(i).append(" FETCH (");

                List<File> files = mailbox.listMails();
                File file = files.get(i - 1);
                int uid = MailStorage.extractUID(file);
                response.append("UID ").append(uid);

                if(dataItems.contains("RFC822.SIZE"))
                    response.append(" RFC822.SIZE ").append(size);  


                if(dataItems.contains("FLAGS")) {
                    response.append(" FLAGS (").append(email.printFlags()).append(")");
                }

                boolean isBodyPeek = dataItems.contains("BODY.PEEK");
                boolean isBody = dataItems.contains("BODY") && !isBodyPeek;

                if(dataItems.contains("BODY[]")) {
                    response.append(" ").append("BODY[]").append("{").append(size).append("}\n");
                    response.append(email.printIMAP()).append(")");
                    sendToClient(response.toString());

                    List<IMAPFlag> flags = email.getFlags();
                    if(!flags.contains(IMAPFlag.SEEN))
                        flags.add(IMAPFlag.SEEN);
                    email.setFlags(flags);
                }
                else if(isBody || isBodyPeek)
                {
                    String bodyTag = isBodyPeek ? "BODY.PEEK[" : "BODY[";
                    int start = dataItems.indexOf("[");
                    int end = dataItems.lastIndexOf("]");
                    String fields = null;
                    if (start != -1 && end != -1 && start < end)
                        fields = dataItems.substring(start + 1, end);
                    
                    StringBuilder body = new StringBuilder();
                    if(fields.contains("From"))
                        body.append("From: ").append(email.getFrom()).append("\r\n");
                    if(fields.contains("To"))
                        body.append("To: ").append(email.getTo()).append("\r\n");
                    if(fields.contains("Subject"))
                        body.append("Subject: ").append(email.getSubject()).append("\r\n");
                    if(fields.contains("Date"))
                        body.append("Date: ").append(email.getDate().toString()).append("\r\n");
                    
                    int sizeFields = body.toString().getBytes(StandardCharsets.UTF_8).length;

                    response.append(" ").append(bodyTag).append(fields).append("] {").append(sizeFields).append("}\n");
                    response.append(body).append(")");
                    sendToClient(response.toString());

                    if(isBody) {
                        List<IMAPFlag> flags = email.getFlags();
                        if(!flags.contains(IMAPFlag.SEEN))
                            flags.add(IMAPFlag.SEEN);
                        email.setFlags(flags);
                    }
                }
                else {
                    response.append(")");
                    sendToClient(response.toString());
                }
            }
            catch(IOException e) {sendBAD(tag, "Error while getting mails");}
        }

        sendCompleted(tag, "FETCH");
    }

    private List<IMAPFlag> parseFlags(String flags) {
        List<IMAPFlag> listFlags = new ArrayList<>();

        flags = flags.trim();
        if(flags.startsWith("(") &&  flags.endsWith(")"))
            flags = flags.substring(1, flags.length() - 1);

        String[] splits = flags.split("\\s+");
        for(String split : splits) {
            split = split.trim();
            if(!split.isEmpty() && split.startsWith("\\")) {
                split = split.substring(1);
                IMAPFlag newFlag = IMAPFlag.correspondingFlag(split);
                if(newFlag != null)
                    listFlags.add(newFlag);
            }
        }

        return listFlags;
    }

    private void handleSTORE(String tag, String arguments) {
        String[] splits = arguments.split(" ", 3);
        if(splits.length < 3) {
            sendBAD(tag, "Invalid STORE syntax");
            return;
        }
        String range = splits[0];
        List<Integer> realRange = computeRange(tag, range);
        String operation = splits[1].toUpperCase();
        String flags = splits[2];
        List<IMAPFlag> realFlags = parseFlags(flags);

        for(int index : realRange) {
            try {
                Email mail = mailbox.getMail(index);
                List<IMAPFlag> flagsMail = mail.getFlags();
                if(operation.startsWith("+")) {
                    for(IMAPFlag flag : realFlags)
                        flagsMail.add(flag);
                    mail.setFlags(flagsMail);
                }
                else if(operation.startsWith("-")) {
                    for(IMAPFlag flag : realFlags)
                        flagsMail.remove(flag);
                    mail.setFlags(flagsMail);
                }
                else
                    mail.setFlags(realFlags);
            }
            catch(IOException e) {sendBAD(tag, "Error while getting mails");}
        }

        sendCompleted(tag, "STORE");
    }

    private int expungeMails(String tag) {
        int nbrDeleted = 0;
        try {
            List<Email> mails = mailbox.getAllMails();

            int index = 1;
            for(Email email : mails) {
                if(email.isFlagDeleted()) {
                    mailbox.deleteMail(index);
                    nbrDeleted += 1;
                }
                index += 1;
            }
        }
        catch(IOException e) {sendBAD(tag, "Error while getting mails");}

        return nbrDeleted;
    }

    private void handleEXPUNGE(String tag) {
        int nbrDeleted = expungeMails(tag);
        sendToClient(tag + " OK EXPUNGE completed, " + nbrDeleted + " messages removed");
    }

    private void handleCLOSE(String tag) {
        expungeMails(tag);
        mailbox = null;
        sendCompleted(tag, "CLOSE");
    }
}
