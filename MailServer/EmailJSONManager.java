import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EmailJSONManager {

    public static void writeEmailToJson(Email email, String filePath) throws IOException {
        System.out.println("filepath:" +filePath);
        File file = new File(filePath);
        File parentDir = file.getParentFile();

        if (!parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Impossible to create file: " + parentDir.getAbsolutePath());
            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"from\": \"").append(email.getFrom()).append("\",\n");
        json.append("  \"to\": \"").append(email.getTo()).append("\",\n");
        json.append("  \"subject\": \"").append(email.getSubject()).append("\",\n");
        json.append("  \"body\": \"").append(email.getBody()).append("\",\n");
        json.append("  \"date\": \"").append(sdf.format(email.getDate() != null ? email.getDate() : new Date())).append("\"\n");
        json.append("}");

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(json.toString());
        }
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

        String from = extractJsonValue(json, "from");
        String to = extractJsonValue(json, "to");
        String subject = extractJsonValue(json, "subject");
        String body = extractJsonValue(json, "body");
        String dateStr = extractJsonValue(json, "date");

        // Parse the date
        Date date = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            date = sdf.parse(dateStr);
        } catch (Exception e) {
            date = new Date();
        }

        return new Email(from, to, subject, body, date);
    }

    private static String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\": \"";
        int start = json.indexOf(pattern);
        if (start == -1) return "";
        start += pattern.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end);
    }
}
