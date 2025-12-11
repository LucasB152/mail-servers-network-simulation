import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;

public class SMTPClientHandler extends AbstractClientHandler {

    //Represent the next command that the server is waiting for
    private SMTPState nextState = SMTPState.HELO;
    private Email email = null;
    private final StringBuilder dataBuffer = new StringBuilder();
    private String ipAddressDestination = null;

    public SMTPClientHandler(Socket socket, String domain) {
        super(socket, domain);
    }

    @Override
    public void analyzeMessage(String message) {
        switch (SMTPClientMessage.match(message)) {
            case SMTPClientMessage.HELO -> handleHeloMessage();
            case SMTPClientMessage.MAIL_FROM -> handleMailFromMessage(message);
            case SMTPClientMessage.RCPT_TO -> handleRcptToMessage(message);
            case SMTPClientMessage.DATA -> handleDataMessage();
            case SMTPClientMessage.QUIT -> handleQuitMessage();
            case null -> handleOtherMessage(message);
        }
    }

    private void handleHeloMessage() {
        if(nextState == SMTPState.HELO){
            this.email = new Email();
            //TODO: vérifier les règles que doivent suivrent les messages HELO
            sendToClient(SMTPServerMessage.GREETING.getFullMessage("mail."+this.domain, this.domain));
            this.nextState = SMTPState.MAIL_FROM;
        }else{
            sendToClient(SMTPServerMessage.BAD_SEQUENCE_COMMAND.getFullMessage());
        }
    }

    private void handleMailFromMessage(String message) {
        if(nextState == SMTPState.MAIL_FROM){
            String email = extractEmail(message);
            sendToClient(verifyEmailValidy(email).getFullMessage());
            this.nextState = SMTPState.RCPT_TO;
        }else{
            sendToClient(SMTPServerMessage.BAD_SEQUENCE_COMMAND.getFullMessage());
        }
    }

    private void handleRcptToMessage(String message){
        if(nextState == SMTPState.RCPT_TO){
            String email = extractEmail(message);
            String domain = extractDomain(email)[1];
            if(domain.equals(this.domain)){
                this.email.setTo(email);
                this.nextState = SMTPState.DATA;
                sendToClient(SMTPServerMessage.OK.getFullMessage());
            }else{
                this.ipAddressDestination = dnsLookup(domain);
                if(this.domain != null){
                    this.email.setTo(email);
                    this.nextState = SMTPState.DATA;
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
        if(nextState == SMTPState.DATA){
            sendToClient(SMTPServerMessage.START_MAIL_INPUT.getFullMessage());
            this.nextState = SMTPState.DATA_READING;
        }else{
            sendToClient(SMTPServerMessage.BAD_SEQUENCE_COMMAND.getFullMessage());
        }
    }

    private void handleQuitMessage(){
        //TODO vérifier ce qu'il faut faire lorsque le client QUIT, fermer la connexion??
        if(nextState == SMTPState.QUIT){
            //Reinitialize for another mail
            this.email = null;
            this.dataBuffer.setLength(0);
            this.nextState = SMTPState.HELO;
            sendToClient(SMTPServerMessage.BYE.getFullMessage());
        }else{
            sendToClient(SMTPServerMessage.BAD_SEQUENCE_COMMAND.getFullMessage());
        }
    }
    
    private void handleOtherMessage(String message){
        if(nextState == SMTPState.DATA_READING){
            if(message.equals(".")){
                this.nextState = SMTPState.QUIT;
                this.email.setBody(dataBuffer.toString());
                sendEmail();
                sendToClient(SMTPServerMessage.MESSAGE_DELIVERED.getFullMessage());
            }else if (this.email.getSubject() == null && message.toLowerCase().matches("from:\\s*[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) {
                this.email.setFrom(message.substring(5).trim());
            }else if (this.email.getSubject() == null && message.toLowerCase().matches("to:\\s*[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) {
                this.email.setTo(message.substring(3).trim());
            }else if (this.email.getSubject() == null && message.toLowerCase().matches("subject:\\s*.*")) {
                this.email.setSubject(message.substring(8).trim());
            }else{
                dataBuffer.append(message).append("\r\n");
            }

        }else{
            sendToClient(SMTPServerMessage.SYNTAX_ERROR.getFullMessage());
        }
    }

    private void sendEmail() {
        if(this.ipAddressDestination == null){
            System.out.println("send email");
            // If ipAddressDestination == null, it means that the mail should be delivered on the actual mail server
            String[] receiverData = extractDomain(email.getTo());
            System.out.println(Arrays.toString(receiverData));
            try {
                EmailJSONManager.writeEmailToJson(email, "data/" + receiverData[0] + "/" + receiverData[1] + "/INBOX/");
            } catch (IOException e) {
                sendToClient(SMTPServerMessage.INVALID_SENDER.getFullMessage());
            }

        }else{
            //Otherwise, it's on another mail server
        }
    }

    private SMTPServerMessage verifyEmailValidy(String email) {
        String domain = extractDomain(email)[1];
        if(domain.equals(this.domain)){
            this.email.setFrom(email);
            return SMTPServerMessage.OK;
        }
        return SMTPServerMessage.INVALID_SENDER;
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
            //TODO : Manage exception
            //throw new IllegalArgumentException("Adresse email invalide : " + email);
        }

        String localPart = trimmed.substring(0, atIndex);
        String domain = trimmed.substring(atIndex + 1);

        return new String[] { localPart, domain };
    }

    private void sendEmailtoAnotherSever(Email email, String sender, String receiver){

    }

    /**
     * Execute a DNS lookup using dig command to find the IP address of the given domain.
     * @param domain The domain we need to find the IP address
     * @return The IP address of the given domain, or NULL if the DNS server doesn't know this domain
     */
    private String dnsLookup(String domain){
        //dig +short MX gembloux.uliege.be
        //dig +short mail.gembloux.uliege.be
        try {
            // 1️⃣ Récupère les MX records
            Process mxProcess = Runtime.getRuntime().exec("dig +short MX " + domain);
            BufferedReader mxReader = new BufferedReader(new InputStreamReader(mxProcess.getInputStream()));
            String line;
            String bestMX = null;
            int bestPriority = Integer.MAX_VALUE;

            while ((line = mxReader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Les lignes ont le format : "10 mail.example.com."
                String[] parts = line.split("\\s+");
                if (parts.length < 2) continue;

                int priority = Integer.parseInt(parts[0]);
                String mxDomain = parts[1];
                if (mxDomain.endsWith(".")) mxDomain = mxDomain.substring(0, mxDomain.length() - 1);

                if (priority < bestPriority) {
                    bestPriority = priority;
                    bestMX = mxDomain;
                }
            }

            mxProcess.waitFor();

            if (bestMX == null) {
                System.err.println("Aucun MX trouvé pour " + domain);
                return null;
            }

            // 2️⃣ Récupère l'IP du MX sélectionné
            Process ipProcess = Runtime.getRuntime().exec("dig +short " + bestMX);
            BufferedReader ipReader = new BufferedReader(new InputStreamReader(ipProcess.getInputStream()));
            String ip = ipReader.readLine(); // on prend la première IP
            ipProcess.waitFor();

            if (ip == null) {
                System.err.println("Impossible de résoudre l'IP de " + bestMX);
                return null;
            }

            return ip;

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

}
