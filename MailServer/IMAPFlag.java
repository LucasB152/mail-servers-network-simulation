public enum IMAPFlag {
    SEEN("Seen"), ANSWERED("Answered"), FLAGGED("Flagged"), DELETED("Deleted");

    private final String name;

    IMAPFlag(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

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

    public static IMAPFlag fromString(String s) {
        for (IMAPFlag flag : IMAPFlag.values()) {
            if (flag.getName().equalsIgnoreCase(s)) {
                return flag;
            }
        }
        return null;
    }
}
