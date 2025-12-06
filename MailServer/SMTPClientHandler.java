package MailServer;

import java.net.Socket;

public class SMTPClientHandler extends AbstractClientHandler {
    private static final String HELO_REGEX = "HELO .*";
    private static final String MAIL_FROM_REGEX = "MAIL FROM:.*";
    private static final String RCPT_TO_REGEX = "RCPT TO:.*";
    private static final String DATA_REGEX = "DATA";
    private static final String QUIT_REGEX = "QUIT";

    //Represent the next command that the server is waiting for
    private SMTPState nextState = null;
    private Email email = null;

    public SMTPClientHandler(Socket socket, String domain) {
        super(socket, domain);
    }

    @Override
    public void analyzeMessage(String message){
        if(SMTPMessage.HELO.matches(message)){
            if(nextState == SMTPState.HELO){
                this.email = new Email();
                sendToClient(SMTPMessage.GREETING.getFullMessage("mail."+this.domain, this.domain));
                this.nextState = SMTPState.MAIL_FROM;
            }else{
                sendToClient(SMTPMessage.BAD_SEQUENCE_COMMAND.getFullMessage());
            }
        }
        else if(SMTPMessage.MAIL_FROM.matches(message)){
            if(nextState == SMTPState.MAIL_FROM){
                String email = extractEmail(message);
                sendToClient(verifyEmailValidy(email).getFullMessage());
            }else{
                sendToClient(SMTPMessage.BAD_SEQUENCE_COMMAND.getFullMessage());
            }
        }
        else if(SMTPMessage.RCPT_TO.matches(message)){
            if(nextState == SMTPState.RCPT_TO){
                String email = extractEmail(message);
                String domain = extractDomain(email);
                if(!domain.equals(this.domain) || dnsLookup(domain) == null){
                    sendToClient(SMTPMessage.MAILBOX_UNAVAILABLE.getFullMessage());
                }else{
                    this.email.setTo(email);
                    this.nextState = SMTPState.DATA;
                    sendToClient(SMTPMessage.OK.getFullMessage());
                }
            }else{
                sendToClient(SMTPMessage.BAD_SEQUENCE_COMMAND.getFullMessage());
            }
        }
        else if(SMTPMessage.DATA.matches(message)){
            if(nextState == SMTPState.DATA){

            }else{
                sendToClient(SMTPMessage.BAD_SEQUENCE_COMMAND.getFullMessage());
            }
        }
        else if(SMTPMessage.QUIT.matches(message)){
            if(nextState == SMTPState.DATA){

            }else{
                sendToClient(SMTPMessage.BAD_SEQUENCE_COMMAND.getFullMessage());
            }
        }
        else{
            // If the message correspond to no command and the waiting command is QUIT, it means that the received message is part of the email body
            if(nextState == SMTPState.QUIT){

            }else{
                sendToClient(SMTPMessage.SYNTAX_ERROR.getFullMessage());
            }
        }
    }

    private SMTPMessage verifyEmailValidy(String email) {
        String domain = extractDomain(email);
        if(domain.equals(this.domain)){
            this.email.setFrom(email);
            return SMTPMessage.OK;
        }
        return SMTPMessage.INVALID_SENDER;
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

    public static String extractDomain(String email) {
        String trimmed = email.trim();
        return trimmed.substring(trimmed.lastIndexOf('@') + 1);
    }


    /**
     * Execute a DNS lookup using dig command to find the IP address of the given domain.
     * @param domain The domain we need to find the IP address
     * @return The IP address of the given domain, or NULL if the DNS server doesn't know this domain
     */
    private String dnsLookup(String domain){
        //dig +short MX gembloux.uliege.be
        //dig +short mail.gembloux.uliege.be
        return null;
    }

}
