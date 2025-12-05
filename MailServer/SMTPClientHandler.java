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

    public SMTPClientHandler(Socket socket) {
        super(socket);
    }

    @Override
    public void analyzeMessage(String message){
        if(message.matches(HELO_REGEX)){
            if(nextState == SMTPState.HELO){

            }else{
                sendToClient(SMTPResponse.BAD_SEQUENCE_COMMAND.getFullMessage());
            }
        }
        else if(message.matches(MAIL_FROM_REGEX)){
            if(nextState == SMTPState.MAIL_FROM){

            }else{
                sendToClient(SMTPResponse.BAD_SEQUENCE_COMMAND.getFullMessage());
            }
        }
        else if(message.matches(RCPT_TO_REGEX)){
            if(nextState == SMTPState.RCPT_TO){

            }else{
                sendToClient(SMTPResponse.BAD_SEQUENCE_COMMAND.getFullMessage());
            }
        }
        else if(message.matches(DATA_REGEX)){
            if(nextState == SMTPState.DATA){

            }else{
                sendToClient(SMTPResponse.BAD_SEQUENCE_COMMAND.getFullMessage());
            }
        }
        else if(message.matches(QUIT_REGEX)){
            if(nextState == SMTPState.DATA){

            }else{
                sendToClient(SMTPResponse.BAD_SEQUENCE_COMMAND.getFullMessage());
            }
        }
        else{
            // If the message correspond to no command and the waiting command is QUIT, it means that the received message is part of the email body
            if(nextState == SMTPState.QUIT){

            }else{
                sendToClient(SMTPResponse.SYNTAX_ERROR.getFullMessage());
            }
        }
    }
}
