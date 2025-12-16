/**
 * Utility class used for user authentication.
 * It provides basic verification of predefined users and passwords.
 *
 * @author Lucas BAUDUIN & Lucas GIOT
 */
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

    /**
     * Checks whether a user exists in the predefined user list.
     *
     * @param username email address of the user
     * @return {@code true} if the user exists, {@code false} otherwise
     */
    public static boolean userExists(String username) {
        for(String user : USERS) {
            if(user.equals(username))
                return true;
        }
        return false;
    }

    /**
     * Verifies the provided password.
     *
     * @param password password provided by the client
     * @return {@code true} if the password is correct, {@code false} otherwise
     */
    public static boolean goodPassword(String password)
    {
        return PASSWORD.equals(password);
    }
}
