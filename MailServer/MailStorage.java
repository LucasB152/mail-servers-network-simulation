import java.io.*;
import java.util.*;

public class MailStorage {
    private static final String ROOTDIR = "data";
    private String user;
    private String domain;
    private String currentDir;

    public MailStorage(String username) {
        String[] splits = username.split("@", 2);
        user = splits[0];
        domain = splits[1];
        currentDir = "INBOX";
        if(!mailboxExists(currentDir))
            createMailbox(currentDir);
    }

    public String getCurrentMailbox() {
        return currentDir;
    }

    private File getUserDir() {
        return new File(ROOTDIR + "/" + domain + "/" + user);
    }

    private File getCurrentDir() {
        return new File(ROOTDIR + "/" + domain + "/" + user + "/" + currentDir);
    }

    private File getAnyDir(String mailboxName) {
        return new File(ROOTDIR + "/" + domain + "/" + user + "/" + mailboxName);
    }

    public static int extractUID(File file) {
        String name = file.getName();
        return Integer.parseInt(name.substring(4, name.length() - 5));
    }

    public List<String> listMailBoxes() {
        List<String> mailboxes = new ArrayList<>();
        
        File userDir = getUserDir();
        if (userDir.exists()) {
            File[] dirs = userDir.listFiles(File::isDirectory);
            if (dirs != null) {
                for (File dir : dirs) {
                    String mailboxName = dir.getName();
                    mailboxes.add(mailboxName);
                }
            }
        }
        
        return mailboxes;
    }

    public boolean mailboxExists(String mailboxName) {
        File mailboxDir = getAnyDir(mailboxName);
        return mailboxDir.exists();
    }

    public boolean createMailbox(String mailboxName) {
        File mailboxDir = getAnyDir(mailboxName);
        if(mailboxExists(mailboxName))
            return false;
        
        boolean created = mailboxDir.mkdirs();
        if(created)
            createMailboxJSON(mailboxDir);
        return created;
    }

    private int createMailboxJSON(File mailbox) {
        int uidValidity = (int) (System.currentTimeMillis() / 1000);

        String json = "{\n" +
        "  \"uidValidity\": " + uidValidity + ",\n" +
        "  \"nextUid\": 1\n" +
        "}";

        File jsonFile = new File(mailbox, "mailbox_metadata.json");
        try(FileWriter writer = new FileWriter(jsonFile)) {
            writer.write(json);
        }
        catch(Exception e) {e.printStackTrace();}

        return uidValidity;
    }

    public void selectMailbox(String mailboxName) {
        currentDir = mailboxName;
    }

    public boolean deleteMailbox(String mailboxName) {
        if(mailboxName.equalsIgnoreCase("INBOX")) 
            return false;

        File mailboxDir = getAnyDir(mailboxName);
        if(!mailboxExists(mailboxName))
            return false;

        File[] files = mailboxDir.listFiles();
        if(files != null) {
            for(File file : files) {
                if(!file.delete())
                    return false;
            }
        }

        return mailboxDir.delete();
    }

    public boolean renameMailbox(String oldName, String newName) {
        if(oldName.equalsIgnoreCase("INBOX")) 
            return false;

        File oldDir = getAnyDir(oldName);
        File newDir = getAnyDir(newName);
        if(!oldDir.exists() || newDir.exists())
            return false;

        boolean renamed = oldDir.renameTo(newDir);
        return renamed;
    }

    public List<File> listMails() {
        File inbox = getCurrentDir();
        if(!inbox.exists())
            return new ArrayList<>();

        File[] files = inbox.listFiles((d, name) -> name.startsWith("msg_") && name.endsWith(".json"));
        List<File> list = new ArrayList<>();
        if(files != null) {
            for(File file : files) {
                list.add(file);
            }
        }

        if(!list.isEmpty())
            list.sort(Comparator.comparingInt(MailStorage::extractUID));
        return list;
    }

    public int getUIDValidity() {
        File mailbox = getCurrentDir();
        File jsonFile = new File(mailbox, "mailbox_metadata.json");
        
        if(!jsonFile.exists()) {
            int uidValidity = createMailboxJSON(mailbox);
            return uidValidity;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(jsonFile))) {
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

    public List<Email> getAllMails() throws IOException{
        List<File> files = listMails();
        List<Email> list = new ArrayList<>();

        for(File file : files)
            list.add(EmailJSONManager.readEmailFromJson(file.getAbsolutePath()));

        return list;
    }

    public void deleteMail(int index) {
        List<File> files = listMails();
        if(index < 1 || index > files.size())
            return;

        files.get(index - 1).delete();
    }
}
