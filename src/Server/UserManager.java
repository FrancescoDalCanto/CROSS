package Server;

public class UserManager {

    /**
     * Restituisce true se l'utente è loggato (cioè se esiste una sessione attiva).
     */
    public static boolean isUserLoggedIn(String username) {
        return SessionManager.isLoggedIn(username);
    }

    /**
     * Effettua il login dell'utente: se non è già loggato, crea e aggiunge una sessione.
     * @param username Lo username dell'utente.
     * @param clientSocket Il socket del client.
     * @return true se il login è andato a buon fine, false altrimenti.
     */
    public static boolean loginUser(String username, java.net.Socket clientSocket) {
        if (SessionManager.isLoggedIn(username)) {
            return false; // L'utente è già loggato
        }
        Session session = new Session(username, clientSocket);
        SessionManager.addSession(username, session);
        return true;
    }

    /**
     * Effettua il logout rimuovendo la sessione associata allo username.
     * @param username Lo username dell'utente.
     * @return true se il logout è andato a buon fine, false se l'utente non era loggato.
     */
    public static boolean logoutUser(String username) {
        if (!SessionManager.isLoggedIn(username)) {
            return false; // Utente non loggato
        }
        SessionManager.removeSession(username);
        return true;
    }
}
