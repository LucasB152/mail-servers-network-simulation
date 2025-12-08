import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MailServer {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java MailServer <domain> <maxThreads>");
            return;
        }

        String domain = args[0];
        int maxThreads = Integer.parseInt(args[1]);
        ExecutorService threadPool = Executors.newFixedThreadPool(maxThreads);

        /*ProtocolServer smtpServer = new ProtocolServer(25, threadPool, domain);
        ProtocolServer pop3Server = new ProtocolServer(110, threadPool, domain);
        ProtocolServer imapServer = new ProtocolServer(143, threadPool, domain);*/

        new Thread(() -> {
            ProtocolServer smtpServer = new ProtocolServer(25, threadPool, domain);
        }, "SMTP-Server").start();

        new Thread(() -> {
            ProtocolServer pop3Server = new ProtocolServer(110, threadPool, domain);
        }, "POP3-Server").start();

        new Thread(() -> {
            ProtocolServer imapServer = new ProtocolServer(143, threadPool, domain);
        }, "IMAP-Server").start();
    }
}
