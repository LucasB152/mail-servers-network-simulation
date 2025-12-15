import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class handles the message received by an SMTP client.
 * Every client has his own ClientHandler running in a separated thread.
 * @author Lucas BAUDUIN & Lucas GIOT
 */
public class SMTPClientHandler extends AbstractClientHandler {

    //Represent the next command that the server is waiting for
    private SMTPState nextState = SMTPState.HELO;
    private Email email = null;
    private final StringBuilder dataBuffer = new StringBuilder();
    private final Map<String, String> recipients = new HashMap<>();
    private final SMTPMailForwarder smtpMailForwarder;
    private final List<String> PREDIFINED_USERS = List.of(
            "dcd@gembloux.uliege.be",
            "vj@gembloux.uliege.be",
            "dcd@info.uliege.be",
            "vj@info.uliege.be",
            "dcd@uliege.be",
            "vj@uliege.be"
    );

    public SMTPClientHandler(Socket socket, String domain, Map<String, String> knownServers) {
        super(socket, domain);
        this.smtpMailForwarder = new SMTPMailForwarder(knownServers);
        System.out.println("Client Handler created");
    }
    @Override
    public void analyzeMessage(String message) {
        SMTPClientMessage smtpClientMessage = SMTPClientMessage.match(message);

        if (smtpClientMessage == null) {
            handleOtherMessage(message);
            return;
        }

        switch (smtpClientMessage) {
            case HELO -> handleHeloMessage(message);
            case MAIL_FROM -> handleMailFromMessage(message);
            case RCPT_TO -> handleRcptToMessage(message);
            case DATA -> handleDataMessage();
            case QUIT -> handleQuitMessage();
            default -> handleOtherMessage(message);
        }
    }


    private void handleHeloMessage(String message) {
        if(nextState == SMTPState.HELO){
            String clientDomain = message.substring(5);

            sendToClient(SMTPServerMessage.GREETING.getFullMessage("mail."+this.domain, clientDomain));
            this.nextState = SMTPState.MAIL_FROM;
        }else{
            sendToClient(SMTPServerMessage.BAD_SEQUENCE_COMMAND.getFullMessage());
        }
    }

    private void handleMailFromMessage(String message) {
        if(nextState == SMTPState.MAIL_FROM){
            String email = extractEmail(message);
            if(this.PREDIFINED_USERS.contains(email)){
                this.email = new Email();
                this.email.setMailFrom(email);
                this.nextState = SMTPState.RCPT_TO;
                sendToClient(SMTPServerMessage.OK.getFullMessage());
            }else{
                sendToClient(SMTPServerMessage.INVALID_SENDER.getFullMessage());
            }
        }else{
            sendToClient(SMTPServerMessage.BAD_SEQUENCE_COMMAND.getFullMessage());
        }
    }

    private void handleRcptToMessage(String message){
        if(nextState == SMTPState.RCPT_TO){
            String email = extractEmail(message);
            String domain = extractDomain(email)[1];

            // If the recipient has the same domain as the sender, then there is no forwarding to perform
            if(domain.equals(this.domain)){
                sendToClient(SMTPServerMessage.OK.getFullMessage());
                this.recipients.put(email, "localhost");
            }else{
                // If it's a different domain, we first need to find the ip address of the recipient domain
                String ipAddressDestination = smtpMailForwarder.findIpAddress(domain);
                if(ipAddressDestination != null){
                    this.recipients.put(email, ipAddressDestination);
                    sendToClient(SMTPServerMessage.OK.getFullMessage());
                }else{
                    sendToClient(SMTPServerMessage.MAILBOX_UNAVAILABLE.getFullMessage());
                }
            }
        }else{
            sendToClient(SMTPServerMessage.BAD_SEQUENCE_COMMAND.getFullMessage());
        }
    }

    private void handleDataMessage(){
        System.out.println("State: " + nextState);
        System.out.println("Size of map: " + this.recipients.size());
        if(nextState == SMTPState.RCPT_TO && !this.recipients.isEmpty()){
            sendToClient(SMTPServerMessage.START_MAIL_INPUT.getFullMessage());
            this.nextState = SMTPState.DATA_READING;
        }else{
            sendToClient(SMTPServerMessage.BAD_SEQUENCE_COMMAND.getFullMessage());
        }
    }

    private void handleQuitMessage(){
        sendToClient(SMTPServerMessage.BYE.getFullMessage());
        super.closeConnection();
    }
    
    private void handleOtherMessage(String message){
        if(nextState == SMTPState.DATA_READING){
            if(message.equals(".")){
                this.email.setBody(dataBuffer.toString().trim());
                sendEmail();
                sendToClient(SMTPServerMessage.MESSAGE_DELIVERED.getFullMessage());
                //Reinitialize for another mail
                this.dataBuffer.setLength(0);
                this.nextState = SMTPState.MAIL_FROM;
            }else if (this.email.getSubject() == null && message.toLowerCase().matches("From:\\s*[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) {
                this.email.setFrom(message.substring(5).trim());
            }else if (this.email.getSubject() == null && message.toLowerCase().matches("To:\\s*[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) {
                this.email.setTo(message.substring(3).trim());
            }else if (this.email.getSubject() == null && message.toLowerCase().matches("Subject:\\s*.*")) {
                this.email.setSubject(message.substring(8).trim());
            }else{
                dataBuffer.append(message).append("\r\n");
            }

        }else{
            sendToClient(SMTPServerMessage.SYNTAX_ERROR.getFullMessage());
        }
    }

    private void sendEmail() {
        for(String email : this.recipients.keySet()){
            String recipientIpAddress = this.recipients.get(email);
            if(recipientIpAddress.equals("localhost")){
                String[] receiverData = extractDomain(email);
                try {
                    EmailJSONManager.writeEmailToJson(this.email, "data/" + receiverData[1] + "/" + receiverData[0] + "/INBOX/");
                }
                catch (IOException e) {
                    sendToClient(SMTPServerMessage.INVALID_SENDER.getFullMessage());
                }
            }else{
                this.email.setRcptTo(email);
                smtpMailForwarder.forwardEmail(this.email, this.recipients.get(email));
            }
        }
    }

    private String extractEmail(String line) {
        int start = line.indexOf('<');
        int end = line.indexOf('>');
        if (start != -1 && end != -1 && end > start) {
            return line.substring(start + 1, end);
        }
        //This should never be returned because the command was correct so the email had the correct format
        return "";
    }

    public static String[] extractDomain(String email) {
        String trimmed = email.trim();
        int atIndex = trimmed.lastIndexOf('@');

        if (atIndex <= 0 || atIndex == trimmed.length() - 1) {
            System.err.println("Invalid email address : " + email);
        }

        String localPart = trimmed.substring(0, atIndex);
        String domain = trimmed.substring(atIndex + 1);

        return new String[] { localPart, domain };
    }
}
