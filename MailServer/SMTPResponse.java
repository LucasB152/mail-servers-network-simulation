package MailServer;

/**
 * Represent the different types of SMTP messages that the server can send to a client.
 * @author Lucas BAUDUIN & Lucas GIOT
 */
public enum SMTPResponse {
    SERVICE_READY(220, "Service ready"),
    OK(250, "OK"),
    START_MAIL_INPUT(354, "End data with <CR><LF>.<CR><LF>"),
    SERVICE_UNAVAILABLE(421, "Service unavailable, closing connection"),
    SYNTAX_ERROR(501, "Syntax error in parameters or arguments"),
    // I add this code response because it's used if the messages are sent in a wrong order.
    BAD_SEQUENCE_COMMAND(503, "503 Bad sequence of commands"),
    MAILBOX_UNAVAILABLE(550, "Requested action not taken: mailbox unavailable"),
    INVALID_SENDER(553, "Requested action not token: invalid sender");

    private final int codeResponse;
    private final String message;

    SMTPResponse(int codeResponse, String message) {
        this.codeResponse = codeResponse;
        this.message = message;
    }

    public String getFullMessage() {
        return codeResponse + " " + message + "\r\n";
    }

    /**
     * Use to retrieve the SERVICE_READY message with the actual domain.
     * @param domain The domain name of the server.
     * @return The SERVICE_READY message formatted with the server's domain.
     */
    public String getFullMessage(String domain) {
        return codeResponse + " domain" + message + "\r\n";
    }
}
