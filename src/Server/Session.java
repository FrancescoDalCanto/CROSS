package Server;

import java.net.Socket;
import java.time.Instant;

public class Session {
    private String username;
    private Socket clientSocket;
    private Instant lastActivity;

    public Session(String username, Socket clientSocket) {
        this.username = username;
        this.clientSocket = clientSocket;
        this.lastActivity = Instant.now();
    }

    public String getUsername() {
        return username;
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    /**
     * Aggiorna il timestamp dell'ultima attività.
     */
    public void updateActivity() {
        this.lastActivity = Instant.now();
    }

    /**
     * Restituisce true se la sessione è scaduta, in base al timeout specificato.
     * @param timeoutMillis Timeout in millisecondi.
     */
    public boolean isExpired(long timeoutMillis) {
        return Instant.now().toEpochMilli() - lastActivity.toEpochMilli() > timeoutMillis;
    }

    public AutoCloseable getSocket() {
        return clientSocket;
    }
}
