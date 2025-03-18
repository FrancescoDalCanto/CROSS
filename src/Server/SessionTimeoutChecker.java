package Server;

public class SessionTimeoutChecker implements Runnable {

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(60 * 1000); // Controllo ogni minuto
                SessionManager.checkSessionTimeouts();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
