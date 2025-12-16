import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
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
    //private IMAPFlag[] flags = new IMAPFlag[0];
    List<IMAPFlag> flags = new ArrayList<>();
    private String from;
    private String to;
    private String subject;
    private String body;
    private Date date;

    public Email() {}

    public Email(String from, String to, List<IMAPFlag> flags, String subject, String body, Date date) {
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

    public List<IMAPFlag> getFlags() {
        return flags;
    }

    public void setFlags(List<IMAPFlag> flags) {
        this.flags = flags;
    }

    public boolean isFlagDeleted() {
        for(IMAPFlag flag : flags) {
            if(flag.equals(IMAPFlag.DELETED))
                return true;
        }
        return false;
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

    public String printIMAP() {
        StringBuilder response = new StringBuilder();
        response.append("From: ").append(from).append("\n");
        response.append("To: ").append(to).append("\n");
        response.append("Subject: ").append(subject).append("\n\n");
        response.append(body);
        return response.toString();
    }

    public String printFlags() {
        StringBuilder response = new StringBuilder();
        for(IMAPFlag flag : flags) 
            response.append("\\").append(flag).append(" ");
        return response.toString();
    }
}
