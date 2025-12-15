import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * This class represents an email.
 * There is a difference between "mailFrom" attribute and "from" attribute (same for "rcptTo" and "to"):
 * "mailFrom" is the SMTP envelope sender, while "from" is the email header visible to the recipient.
 * @author Lucas BAUDUIN & Lucas GIOT
 */
public class Email {
    private String mailFrom;
    private String rcptTo;

    //When the Email is created, there is no flags
    private IMAPFlag[] flags = new IMAPFlag[0];
    private String from;
    private String to;
    private String subject;
    private String body;
    private Date date;

    public Email() {}

    public Email(String from, String to, IMAPFlag[] flags, String subject, String body, Date date) {
        this.from = from;
        this.to = to;
        this.flags = flags;
        this.subject = subject;
        this.body = body;
        this.date =date;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public String getRcptTo() {
        return rcptTo;
    }

    public void setRcptTo(String rcptTo) {
        this.rcptTo = rcptTo;
    }

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

    public Date getDate() {
        return date;
    }

    public IMAPFlag[] getFlags() {
        return flags;
    }

    public void setFlags(IMAPFlag[] flags) {
        this.flags = flags;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getSize() {
        int size = from.getBytes(StandardCharsets.UTF_8).length;
        size += to.getBytes(StandardCharsets.UTF_8).length;
        size += subject.getBytes(StandardCharsets.UTF_8).length;
        size += body.getBytes(StandardCharsets.UTF_8).length;
        return size;
    }

    @Override
    public String toString() {
        return "Email{" +
                "mailFrom='" + mailFrom + '\'' +
                ", rcptTo='" + rcptTo + '\'' +
                ", from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", subject='" + subject + '\'' +
                ", body='" + body + '\'' +
                ", date=" + date +
                '}';
    }
}
