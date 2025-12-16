import java.io.*;
import java.util.*;

/**
 * Manages persistent storage of user mailboxes and emails.
 * This class provides mailbox and message operations used by POP3 and IMAP.
 *
 * @author Lucas BAUDUIN & Lucas GIOT
 */
public class MailStorage {
    private static final String ROOTDIR = "data";
    private String user;
    private String domain;
    private String currentDir;

    /**
     * Creates a mail storage instance for a given user.
     *
     * @param username full email address of the user
     */
    public MailStorage(String username) {
        String[] splits = username.split("@", 2);
        user = splits[0];
        domain = splits[1];
        currentDir = "INBOX";
    }

    /**
     * @return name of the currently selected mailbox
     */
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

    private static int extractUID(File file) {
        String name = file.getName();
        return Integer.parseInt(name.substring(4, name.length() - 5));
    }

    /**
     * Lists all mailboxes of the current user.
     *
     * @return list of mailbox names
     */
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

    /**
     * Checks whether a mailbox exists.
     *
     * @param mailboxName mailbox name
     * @return {@code true} if the mailbox exists
     */
    public boolean mailboxExists(String mailboxName) {
        File mailboxDir = getAnyDir(mailboxName);
        return mailboxDir.exists();
    }

    /**
     * Creates a new mailbox.
     *
     * @param mailboxName mailbox name
     * @return {@code true} if creation succeeds
     */
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

        File jsonFile = new File(mailbox, "mailbox.json");
        try(FileWriter writer = new FileWriter(jsonFile)) {
            writer.write(json);
        }
        catch(Exception e) {e.printStackTrace();}

        return uidValidity;
    }

    /**
     * Selects the mailbox used for subsequent operations.
     * @param mailboxName mailbox name
     */
    public void selectMailbox(String mailboxName) {
        currentDir = mailboxName;
    }

    /**
     * Deletes a mailbox and all its contents.
     * @param mailboxName mailbox name
     * @return {@code true} if deletion succeeds
     */
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

    /**
     * Renames a mailbox.
     * @param oldName original mailbox name
     * @param newName new mailbox name
     * @return {@code true} if rename succeeds
     */
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

    /**
     * Lists all email files in the current mailbox.
     * @return list of email files
     */
    public List<File> listMails() {
        File inbox = getCurrentDir();
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

    /**
     * @return UIDVALIDITY value
     */
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

    /**
     * @return number of emails
     */
    public int getMailsCount() {
        return listMails().size();
    }

    /**
     * Retrieves an email by index.
     *
     * @param index message index
     * @return {@code Email} object
     * @throws IOException if an I/O error occurs
     */
    public Email getMail(int index) throws IOException {
        List<File> files = listMails();
        if(index < 1 || index > files.size())
            return null;

        File file = files.get(index - 1);
        return EmailJSONManager.readEmailFromJson(file.getAbsolutePath());
    }

    /**
     * Retrieves all emails from the current mailbox.
     *
     * @return list of emails
     * @throws IOException if an I/O error occurs
     */
    public List<Email> getAllMails() throws IOException{
        List<File> files = listMails();
        List<Email> list = new ArrayList<>();

        for(File file : files)
            list.add(EmailJSONManager.readEmailFromJson(file.getAbsolutePath()));

        return list;
    }

    /**
     * Permanently deletes an email.
     *
     * @param index message index
     */
    public void deleteMail(int index) {
        List<File> files = listMails();
        if(index < 1 || index > files.size())
            return;

        files.get(index - 1).delete();
    }
}
