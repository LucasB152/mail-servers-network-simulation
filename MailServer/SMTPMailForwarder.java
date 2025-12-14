import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is responsible for forwarding messages from a mail server to another.
 * It's enable to perform a search to find the IP address of other sever, even if it's unknown.
 * @author Lucas BAUDUIN & Lucas GIOT
 */
public class SMTPMailForwarder {
    private Map<String, String> knownServers = new HashMap<>();
    public SMTPMailForwarder(Map<String, String> knownServers) {
        this.knownServers = knownServers;
    }

    public void forwardEmail(Email email, String ipAddress) {
        try (Socket socket = new Socket(ipAddress, 25)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            sendCommand(writer, reader, "HELO localhost");

            sendCommand(writer, reader, "MAIL FROM:<" + email.getMailFrom() + ">");

            sendCommand(writer, reader, "RCPT TO:<" + email.getRcptTo() + ">");

            sendCommand(writer, reader, "DATA");

            // Corps du message
            writer.println("Subject: " + email.getSubject());
            writer.println("From: " + email.getFrom());
            writer.println("To: " + email.getTo());
            writer.println(email.getBody());
            writer.println(".");

            // Lire la réponse finale du serveur
            System.out.println("Serveur: " + reader.readLine());

            // Quitter la session SMTP
            sendCommand(writer, reader, "QUIT");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendCommand(PrintWriter writer, BufferedReader reader, String command) throws IOException {
        writer.println(command);
    }

    public String findIpAddress(String domain) {
        String ipAddress = knownServers.get(domain);
        System.out.println("-----Voici l'adresse IP de info.uliege.be: " + ipAddress);
        return ipAddress == null ? dnsLookup(domain) : ipAddress;
    }

    /**
     * Execute a DNS lookup using dig command to find the IP address of the given domain.
     * @param domain The domain we need to find the IP address
     * @return The IP address of the given domain, or NULL if the DNS server doesn't know this domain
     */
    private String dnsLookup(String domain){
        //dig +short MX gembloux.uliege.be
        //dig +short mail.gembloux.uliege.be
        try {
            // Gets all the MX records in the short form
            Process mxProcess = Runtime.getRuntime().exec("dig +short MX " + domain);
            BufferedReader mxReader = new BufferedReader(new InputStreamReader(mxProcess.getInputStream()));
            String line;
            String bestMX = null;
            int bestPriority = Integer.MAX_VALUE;

            while ((line = mxReader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                System.out.println("Line: " + line);
                // Format example : "10 mail.example.com."
                String[] parts = line.split("\\s+");
                if (parts.length < 2) continue;

                int priority = Integer.parseInt(parts[0]);
                String mxDomain = parts[1];
                if (mxDomain.endsWith(".")) mxDomain = mxDomain.substring(0, mxDomain.length() - 1);

                if (priority < bestPriority) {
                    bestPriority = priority;
                    bestMX = mxDomain;
                }
            }

            mxProcess.waitFor();

            if (bestMX == null) {
                System.err.println("No MX record found for the domain: " + domain);
                return null;
            }

            // Get the IP address of the highest MX
            System.out.println("The best MX is : " + bestMX);
            Process ipProcess = Runtime.getRuntime().exec("dig +short " + bestMX);
            BufferedReader ipReader = new BufferedReader(new InputStreamReader(ipProcess.getInputStream()));
            String ip = ipReader.readLine(); // on prend la première IP
            ipProcess.waitFor();

            if (ip == null) {
                System.err.println("Impossible to resolve the IP address of: " + bestMX);
                return null;
            }

            return ip;

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }
}
