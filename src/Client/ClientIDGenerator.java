package Client;

import java.util.Random;

public class ClientIDGenerator {
    private static int clientId = -1;

    public static int getClientId() {
        if (clientId == -1) {
            // Genera un nuovo ID casuale tra 1 e 1000000000
            clientId = new Random().nextInt(1000000000) + 1;
        }
        return clientId;
    }
}
