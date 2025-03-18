package Client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ClientMain {
    private String host;
    private int port;
    private static final int SERVER_UDP_PORT = 6000; // Porta del server per registrazioni UDP
    private static final int CLIENT_UDP_PORT = 5000; // Porta su cui il client riceve notifiche
    private int userId; // L'ID dell'utente viene recuperato da ClientIDGenerator

    // Costruttore per leggere i dati dal file JSON e ottenere l'ID del client
    public ClientMain() {
        try (FileReader reader = new FileReader("src/Document/Connection.json")) {
            Type tipoMappa = new TypeToken<ConcurrentHashMap<String, Object>>() {}.getType();
            ConcurrentHashMap<String, Object> map = new Gson().fromJson(reader, tipoMappa);

            this.host = map.get("host").toString();
            this.port = (int) Double.parseDouble(map.get("port").toString());

            // Recupera l'ID generato dinamicamente
            this.userId = ClientIDGenerator.getClientId();

            System.out.println("ID del client generato: " + this.userId);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Metodo per registrarsi presso il server per ricevere notifiche UDP
    private void registerClient() {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress serverAddress = InetAddress.getByName(host);

            // Invia il proprio ID al server
            String message = "REGISTER=" + userId;
            byte[] buffer = message.getBytes();

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, SERVER_UDP_PORT);
            socket.send(packet);

            System.out.println("Registrazione inviata al server per l'utente ID: " + userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Metodo per ascoltare le notifiche dal server
    private void listenForNotifications() {
        try (DatagramSocket socket = new DatagramSocket(CLIENT_UDP_PORT)) {
            byte[] buffer = new byte[1024];
            System.out.println("In attesa di notifiche dal server...");

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("📩 Notifica ricevuta: " + message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Metodo per avviare la connessione TCP al server
    public void startClient() throws Exception {
        try (Socket socket = new Socket(host, port)) {
            System.out.println("Connesso al server " + host + " sulla porta " + port);

            // Avvio la CLI per la gestione degli ordini
            CLI cli = new CLI(socket);
            cli.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ClientMain client = new ClientMain();

        // Avvia il listener UDP in un thread separato
        new Thread(() -> client.listenForNotifications()).start();

        // Registra il client per ricevere notifiche
        client.registerClient();

        // Avvia la connessione TCP con il server
        try {
            client.startClient();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
