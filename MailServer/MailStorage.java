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
            for(File file : files) {
                if(!file.getName().equals("mailbox_metadata.json"))
                    list.add(file);
            }
        }

        list.sort(Comparator.comparingInt(MailStorage::extractUID));
        return list;
    }

    public int getUIDValidity() {
        File inbox = getInboxDir();
        File mailboxFile = new File(inbox, "mailbox_metadata.json");
        
        if(!mailboxFile.exists()) {
            int uidValidity = (int) (System.currentTimeMillis() / 1000);

            mailboxFile.getParentFile().mkdirs();
            String json = "{\n" +
            "  \"uidValidity\": " + uidValidity + ",\n" +
            "  \"nextUid\": 1,\n" +
            "}";

            try(FileWriter writer = new FileWriter(mailboxFile)) {
                writer.write(json);
            }
            catch(Exception e) {e.printStackTrace();}

            return uidValidity;
        }

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

    public Email getMail(int index) throws IOException {
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
