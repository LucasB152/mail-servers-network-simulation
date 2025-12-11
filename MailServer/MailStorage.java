import java.io.*;
import java.util.*;

public class MailStorage {
    private static final String ROOTDIR = "data";

    private static File getInboxDir(String username) {
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

    public static List<File> listMails(String username) {
        File userDir = getInboxDir(username);
        if(!userDir.exists())
            return new ArrayList<>();

        File[] files = userDir.listFiles((d, name) -> name.endsWith(".json"));
        List<File> list = new ArrayList<>();
        if(files != null) {
            for(File file : files)
                list.add(file);
        }

        list.sort(Comparator.comparingInt(MailStorage::extractUID));
        return list;
    }

    public static int getMailsCount(String username) {
        return listMails(username).size();
    }

    public static Email readMail(String username, int index) throws IOException {
        List<File> files = listMails(username);
        if(index < 1 || index > files.size())
            return null;

        File file = files.get(index - 1);
        return EmailJSONManager.readEmailFromJson(file.getAbsolutePath());
    }

    public static void deleteMail(String username, int index) {
        List<File> files = listMails(username);
        if(index < 1 || index > files.size())
            return;

        files.get(index - 1).delete();
    }
}
