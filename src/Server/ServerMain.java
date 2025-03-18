package Server;

import CommonClass.CompraVendita;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    private static final File connessione = new File("src/Document/ConnectionServer.json");
    private int port;
    private final SessionManager sessionManager;
    private final ExecutorService threadPool;
    private static CompraVendita compraVendita;

    // Costruttore
    public ServerMain() {
        this.compraVendita = new CompraVendita();
        try {
            if (!connessione.exists()) {
                System.out.println("Connessione.json non trovato");
            }

            try (FileReader fr = new FileReader(connessione)) {
                ConnectionConfig connection = new Gson().fromJson(fr, ConnectionConfig.class);
                this.port = connection.getPort();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.sessionManager = new SessionManager();
        this.threadPool = Executors.newFixedThreadPool(10);

        Thread sessionTimeoutChecker = new Thread(new SessionTimeoutChecker());
        sessionTimeoutChecker.setDaemon(true);
        sessionTimeoutChecker.start();
    }

    // Avvio del server
    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(this.port)) {
            System.out.println("Server in ascolto sulla porta: " + this.port);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                // Passa il gestore dello shutdown a ogni connessione
                CROSSServer serverHandler = new CROSSServer(clientSocket, sessionManager, compraVendita);
                threadPool.execute(serverHandler);
            }
        } catch (IOException e) {
            System.out.println("Errore avvenuto durante l'avvio del server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            ServerMain server = new ServerMain();

            // Avvia il listener per le registrazioni client in un thread separato
            new Thread(() -> compraVendita.listenForRegistrations()).start();

            /* Carica DataBase */
            compraVendita.initializeOrderBook();

            /* Legge ultimo ID degli ordini e lo salva in memoria */


            /* Chiamo CompraVendita.match */
            compraVendita.matching();

            /* Sleep 2000ms */
            Thread.sleep(2000);

            server.startServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
