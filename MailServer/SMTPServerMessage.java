/**
 * Represent the different types of SMTP messages that the server can send to a client.
 * @author Lucas BAUDUIN & Lucas GIOT
 */
public enum SMTPServerMessage {
    SERVICE_READY(220, "%s Service ready"),
    BYE(221, "Bye"),
    GREETING(250, "%s greets %s"),
    OK(250, "OK"),
    MESSAGE_DELIVERED(250, "Message accepted for delivery"),
    START_MAIL_INPUT(354, "End data with <CR><LF>.<CR><LF>"),
    SERVICE_UNAVAILABLE(421, "Service unavailable, closing connection"),
    SYNTAX_ERROR(501, "Syntax error in parameters or arguments"),
    // I add this code response because it's used if the messages are sent in a wrong order.
    BAD_SEQUENCE_COMMAND(503, "Bad sequence of commands"),
    MAILBOX_UNAVAILABLE(550, "Requested action not taken: mailbox unavailable"),
    INVALID_SENDER(553, "Requested action not token: invalid sender");

    private final int codeResponse;
    private final String message;

    SMTPServerMessage(int codeResponse, String message) {
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
}
