/**
 * Represents the different IMAP flags that can be associated with an email message.
 * @author Lucas BAUDUIN & Lucas GIOT
 */
public enum IMAPFlag {
    SEEN("Seen"), ANSWERED("Answered"), FLAGGED("Flagged"), DELETED("Deleted");

    private final String name;

    /**
     * Creates a new IMAPFlag.
     * 
     * @param name name of the flag (4 differents ones)
     */
    IMAPFlag(String name) {
        this.name = name;
    }

    /**
     * Get the name of a IMAPFlag.
     * 
     * @return the name of the flag
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the corresponding IMAP flag for a given string.
     *
     * @param input string representation of the flag
     * @return corresponding {@code IMAPFlag}, or {@code null} if not found
     */
    public static IMAPFlag correspondingFlag(String input) {
        switch (input) {
            case "Seen":
                return SEEN;
            case "Answered":
                return ANSWERED;
            case "Flagged":
                return FLAGGED;
            case "Deleted":
                return DELETED;
            default:
                return null;
        }
    }

    /**
     * Converts a string to an IMAP flag, ignoring case.
     *
     * @param s string representation
     * @return matching {@code IMAPFlag}, or {@code null} if not found
     */
    public static IMAPFlag fromString(String s) {
        for (IMAPFlag flag : IMAPFlag.values()) {
            if (flag.getName().equalsIgnoreCase(s)) {
                return flag;
            }
        }
        return null;
    }
}
