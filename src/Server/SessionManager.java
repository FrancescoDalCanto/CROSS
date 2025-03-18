package Server;

import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final long SESSION_TIMEOUT = 5 * 60 * 1000; // 5 minuti
    private static final Map<String, Session> activeSessions = new ConcurrentHashMap<>();

    /**
     * Aggiunge una nuova sessione associata a uno username.
     */
    public static void addSession(String username, Session session) {
        activeSessions.put(username, session);
    }

    /**
     * Verifica se uno username è già loggato.
     */
    public static boolean isLoggedIn(String username) {
        return activeSessions.containsKey(username);
    }

    /**
     * Rimuove la sessione associata a uno username.
     */
    public static void removeSession(String username) {
        activeSessions.remove(username);
    }

    /**
     * Controlla e rimuove tutte le sessioni scadute.
     */
    public static void checkSessionTimeouts() {
        activeSessions.values().removeIf(session -> session.isExpired(SESSION_TIMEOUT));
    }

    /**
     * Recupera la sessione associata a uno username.
     */
    public static Session getSession(String username) {
        return activeSessions.get(username);
    }

    /**
     * Recupera la sessione in base al socket del client.
     */
    public static Session getSessionFromSocket(Socket socket) {
        for (Session session : activeSessions.values()) {
            if (session.getClientSocket().equals(socket)) {
                return session;
            }
        }
        return null;
    }

    public String[] getAllSessions() {
        return activeSessions.keySet().toArray(new String[0]);
    }
}
