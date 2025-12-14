import java.util.HashMap;
import java.util.Map;
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

        //Hardcode the necessary IP address
        Map<String, String> knownServers = new HashMap<>();
        if(domain.equals("uliege.be") || domain.equals("mail.uliege.be")){
            knownServers.put("info.uliege.be", "10.0.3.7");
        }


        new Thread(() -> {
            ProtocolServer smtpServer = new ProtocolServer(threadPool, 25, domain, knownServers);
            }, "SMTP-Server").start();

        new Thread(() -> {
            ProtocolServer pop3Server = new ProtocolServer(110, threadPool, domain);
        }, "POP3-Server").start();

        new Thread(() -> {
            ProtocolServer imapServer = new ProtocolServer(143, threadPool, domain);
        }, "IMAP-Server").start();
    }
}
