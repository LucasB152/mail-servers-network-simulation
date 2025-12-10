/**
 * Represent the different types of SMTP messages that the server can send to a client.
 * @author Lucas BAUDUIN & Lucas GIOT
 */
public enum SMTPMessage {
    SERVICE_READY(220, "%s Service ready"),
    BYE(221, "Bye"),
    GREETING(250, "%s greets %s"),
    OK(250, "OK"),
    MESSAGE_DELIVERED(250, "Message accepted for delivery"),
    START_MAIL_INPUT(354, "End data with <CR><LF>.<CR><LF>"),
    SERVICE_UNAVAILABLE(421, "Service unavailable, closing connection"),
    SYNTAX_ERROR(501, "Syntax error in parameters or arguments"),
    // I add this code response because it's used if the messages are sent in a wrong order.
    BAD_SEQUENCE_COMMAND(503, "503 Bad sequence of commands"),
    MAILBOX_UNAVAILABLE(550, "Requested action not taken: mailbox unavailable"),
    INVALID_SENDER(553, "Requested action not token: invalid sender"),
    HELO(0, "HELO %s"),
    MAIL_FROM(0, "MAIL FROM:<%s>"),
    RCPT_TO(0, "RCPT TO:<%s>"),
    DATA(0, "DATA"),
    QUIT(0, "QUIT");

    private final int codeResponse;
    private final String message;
    private static final String EMAIL_REGEX = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";

    SMTPMessage(int codeResponse, String message) {
        this.codeResponse = codeResponse;
        this.message = message;
    }

    public String getFullMessage(Object... args) {
        String formattedMessage = String.format(this.message, args);
        if (codeResponse == 0) {
            return formattedMessage;
        }
        return codeResponse + " " + formattedMessage;
    }

    public boolean matches(String receivedMessage) {
        if (this == MAIL_FROM || this == RCPT_TO) {
            String regex = String.format(this.message, EMAIL_REGEX);
            return receivedMessage.matches(regex);
        }
        return message.matches(this.message);
    }
}
