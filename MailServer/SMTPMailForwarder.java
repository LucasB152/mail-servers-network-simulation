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

    /**
     * Creates a new mail forwarder with a set of known mail servers.
     *
     * @param knownServers map of known domains to mail server IP addresses
     */
    public SMTPMailForwarder(Map<String, String> knownServers) {
        this.knownServers = knownServers;
    }

    /**
     * Forwards an email to a remote mail server using SMTP.
     *
     * @param email     email to forward
     * @param ipAddress IP address of the destination mail server
     */
    public void forwardEmail(Email email, String ipAddress) {
        try (Socket socket = new Socket(ipAddress, 25)) {
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            sendCommand(writer, "HELO localhost");
            sendCommand(writer, "MAIL FROM:<" + email.getMailFrom() + ">");
            sendCommand(writer, "RCPT TO:<" + email.getRcptTo() + ">");
            sendCommand(writer, "DATA");

            writer.println("Subject: " + email.getSubject());
            writer.println("From: " + email.getFrom());
            writer.println("To: " + email.getTo());
            writer.println(email.getBody());
            writer.println(".");

            sendCommand(writer, "QUIT");

        } catch (IOException e) {
            System.err.println("Error during the forwarding of the email: " + e.getMessage());
        }
    }

    /**
     * Sends an SMTP command to the remote server.
     *
     * @param writer  output stream to the server
     * @param command SMTP command to send
     * @throws IOException if an I/O error occurs
     */
    private static void sendCommand(PrintWriter writer, String command) throws IOException {
        writer.println(command);
    }

    /**
     * Resolves the IP address of a domain using predefined mappings or DNS lookup.
     *
     * @param domain destination domain
     * @return IP address of the mail server, or {@code null} if unresolved
     */
    public String findIpAddress(String domain) {
        String ipAddress = knownServers.get(domain);
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

                System.out.println("DNS Lookup line: " + line);
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
            System.err.println("Error during the dns lookup: " + e.getMessage());
        }

        return null;
    }
}
