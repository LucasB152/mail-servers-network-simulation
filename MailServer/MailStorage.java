import java.io.*;
import java.util.*;

public class MailStorage {
    private static final String ROOTDIR = "data";
    private String username;

    public MailStorage(String username) {
        this.username = username;
    }

    private File getInboxDir() {
        String[] splits = username.split("@");
        String user = splits[0];
        String domain = splits[1];
        System.out.println("User: " + user + " Domain: " + domain);

        return new File(ROOTDIR + "/" + domain + "/" + user + "/INBOX");
    }

    private static int extractUID(File file) {
        String name = file.getName();
        return Integer.parseInt(name.substring(4, name.length() - 5));
    }

    public List<File> listMails() {
        File inbox = getInboxDir();
        if(!inbox.exists())
            return new ArrayList<>();

        File[] files = inbox.listFiles((d, name) -> name.endsWith(".json"));
        List<File> list = new ArrayList<>();
        if(files != null) {
            for(File file : files)
                list.add(file);
        }

        list.sort(Comparator.comparingInt(MailStorage::extractUID));
        return list;
    }

    public int getUIDValidity() {
        File inbox = getInboxDir();
        File mailboxFile = new File(inbox, "mailbox.json");
        
        //Implementer si mailbox.json n'existe pas

        try (BufferedReader br = new BufferedReader(new FileReader(mailboxFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("\"uidValidity\"")) {
                    String[] splits = line.split(":");
                    String uid = splits[1].replace(",", "").trim();
                    return Integer.parseInt(uid);
                }
            }
        } 
        catch (Exception e) {e.printStackTrace();}

        return 1;
    }

    public int getMailsCount() {
        return listMails().size();
    }

    public Email readMail(int index) throws IOException {
        List<File> files = listMails();
        if(index < 1 || index > files.size())
            return null;

        File file = files.get(index - 1);
        return EmailJSONManager.readEmailFromJson(file.getAbsolutePath());
    }

    public void deleteMail(int index) {
        List<File> files = listMails();
        if(index < 1 || index > files.size())
            return;

        files.get(index - 1).delete();
    }
}
