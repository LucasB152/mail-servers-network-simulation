public class Users {
    private static final String[] USERS = {
        "dcd@gembloux.uliege.be",
        "vj@gembloux.uliege.be",
        "dcd@info.uliege.be",
        "vj@info.uliege.be",
        "dcd@uliege.be",
        "vj@uliege.be"
    };

    private static final String PASSWORD = "password";

    public static boolean userExists(String username) {
        for(String user : USERS) {
            if(user.equals(username))
                return true;
        }
        return false;
    }

    public static boolean goodPassword(String password)
    {
        return PASSWORD.equals(password);
    }
}
