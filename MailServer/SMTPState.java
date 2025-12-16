/**
 * Represents the different states of an SMTP session.
 * It is used to ensure the correct command sequence during message transmission.
 * @author Lucas BAUDUIN & Lucas GIOT
 */
public enum SMTPState {HELO, MAIL_FROM, RCPT_TO, DATA_READING
}
