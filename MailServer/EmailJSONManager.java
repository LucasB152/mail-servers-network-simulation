import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EmailJSONManager {

    private static final String METADATA_FILENAME = "mailbox_metadata.json";

    public static void writeEmailToJson(Email email, String mailboxPath) throws IOException {
        File mailboxDir = new File(mailboxPath);

        if (!mailboxDir.exists() && !mailboxDir.mkdirs()) {
            throw new IOException("Impossible de créer le dossier mailbox : " + mailboxDir.getAbsolutePath());
        }

        // Lire ou créer metadata.json
        File metadataFile = new File(mailboxDir, METADATA_FILENAME);
        long uidNext = 1;
        long uidValidity = System.currentTimeMillis();

        if (metadataFile.exists()) {
            // Lire uidnext et uidvalidity
            try (BufferedReader br = new BufferedReader(new FileReader(metadataFile))) {
                String json = br.readLine().trim();
                uidNext = Long.parseLong(extractJsonValue(json, "uidnext"));
                uidValidity = Long.parseLong(extractJsonValue(json, "uidvalidity"));
            } catch (Exception e) {
                // Si erreur, utiliser 1 et timestamp
                uidNext = 1;
                uidValidity = System.currentTimeMillis();
            }
        } else {
            // Créer metadata.json
            writeMetadata(metadataFile, uidNext, uidValidity);
        }

        // Créer le fichier mail msg_<uid>.json
        File mailFile = new File(mailboxDir, "msg_" + uidNext + ".json");
        writeMailFile(mailFile, email);

        // Incrémenter uidnext et sauvegarder
        writeMetadata(metadataFile, uidNext + 1, uidValidity);
    }

    private static void writeMetadata(File metadataFile, long uidNext, long uidValidity) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"uidvalidity\": ").append(uidValidity).append(",\n");
        json.append("  \"uidnext\": ").append(uidNext).append("\n");
        json.append("}");
        try (FileWriter writer = new FileWriter(metadataFile)) {
            writer.write(json.toString());
        }
    }

    private static void writeMailFile(File file, Email email) throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"from\": \"").append(email.getFrom()).append("\",\n");
        json.append("  \"to\": \"").append(email.getTo()).append("\",\n");
        json.append("  \"flags\": \"").append(email.printFlags()).append("\",\n");
        json.append("  \"subject\": \"").append(email.getSubject()).append("\",\n");
        json.append("  \"body\": \"").append(email.getBody()).append("\",\n");
        json.append("  \"date\": \"").append(sdf.format(email.getDate() != null ? email.getDate() : new Date())).append("\"\n");
        json.append("}");

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(json.toString());
        }
    }

    private static String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return "";
        start += pattern.length();

        // Avancer jusqu'au chiffre
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\"')) start++;

        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;

        return json.substring(start, end);
    }

    public static Email readEmailFromJson(String filePath) throws IOException {
        File file = new File(filePath);
        StringBuilder jsonBuilder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line.trim());
            }
        }

        String json = jsonBuilder.toString();

        String from = extractJsonValueString(json, "from");
        String to = extractJsonValueString(json, "to");
        String subject = extractJsonValueString(json, "subject");
        String body = extractJsonValueString(json, "body");
        String dateStr = extractJsonValueString(json, "date");

        // Get the flags and convert them in IMAPFlag objects
        String[] flagsStr = extractJsonArray(json, "flags");
        List<IMAPFlag> flags = new ArrayList<>();
        for (int i = 0; i < flagsStr.length; i++) {
            flags.add(IMAPFlag.fromString(flagsStr[i]));
        }

        // Parse the date
        Date date = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            date = sdf.parse(dateStr);
        } catch (Exception e) {
            date = new Date();
        }

        return new Email(from, to, flags, subject, body, date);
    }

    private static String extractJsonValueString(String json, String key) {
        String pattern = "\"" + key + "\": \"";
        int start = json.indexOf(pattern);
        if (start == -1) return "";
        start += pattern.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end);
    }

    private static String[] extractJsonArray(String json, String key) {
        String pattern = "\"" + key + "\": [";
        int start = json.indexOf(pattern);
        if (start == -1) return new String[0];
        start += pattern.length();

        int end = json.indexOf("]", start);
        if (end == -1) return new String[0];

        String arrayContent = json.substring(start, end).trim();
        if (arrayContent.isEmpty()) return new String[0];

        // Séparer les éléments par virgule et enlever les guillemets
        String[] items = arrayContent.split(",");
        for (int i = 0; i < items.length; i++) {
            items[i] = items[i].trim().replaceAll("^\"|\"$", "");
        }

        return items;
    }
}
