/**
 * Represents the different SMTP commands that can be sent by a client.
 * Each enum value defines the expected format and validation pattern.
 * @author Lucas BAUDUIN & Lucas GIOT
 */
public enum SMTPClientMessage {
    HELO("HELO %s", "HELO (?:[a-zA-Z0-9.-]+|\\[[0-9.]+\\])"),
    EHLO("EHLO %s", "EHLO (?:[a-zA-Z0-9.-]+|\\[[0-9.]+\\])")
    ,
    MAIL_FROM("MAIL FROM:<%s>", "MAIL FROM:<[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}>"),
    RCPT_TO("RCPT TO:<%s>", "RCPT TO:<[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}>"),
    DATA("DATA", "DATA"),
    QUIT("QUIT", "QUIT");
    private final String message;
    private final String pattern;

    /**
     * Creates a new SMTP client command definition.
     *
     * @param message command format
     * @param pattern regular expression used to validate the command
     */
    SMTPClientMessage(String message, String pattern) {
        this.message = message;
        this.pattern = pattern;
    }

    /**
     * Matches a raw client message against known SMTP command patterns.
     *
     * @param message raw message received from the client
     * @return corresponding {@code SMTPClientMessage} or {@code null} if no match is found
     */
    public static SMTPClientMessage match(String message) {
        for (var type : values()) {
            if (message.matches(type.pattern)) return type;
        }
        return null;
    }
}
