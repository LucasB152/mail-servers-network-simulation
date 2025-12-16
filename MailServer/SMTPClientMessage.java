import java.util.regex.Pattern;

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

    SMTPClientMessage(String message, String pattern) {
        this.message = message;
        this.pattern = pattern;
    }

    public static SMTPClientMessage match(String message) {
        for (var type : values()) {
            if (message.matches(type.pattern)) return type;
        }
        return null;
    }
}
