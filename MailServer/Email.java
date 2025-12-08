import java.nio.charset.StandardCharsets;

public class Email {
    private String from;
    private String to;
    private String subject;
    private String body;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public int getSize() {
        int size = from.getBytes(StandardCharsets.UTF_8).length;
        size += to.getBytes(StandardCharsets.UTF_8).length;
        size += subject.getBytes(StandardCharsets.UTF_8).length;
        size += body.getBytes(StandardCharsets.UTF_8).length;
        return size;
    }
}
