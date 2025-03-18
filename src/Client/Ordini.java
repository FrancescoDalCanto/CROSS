package Client;

import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Ordini {
    private final Gson gson = new Gson();
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private static final AtomicInteger orderIdCounter = new AtomicInteger(1); // ID globale per tutti gli ordini
    private final String currentUsername; // Username autenticato

    public Ordini(String serverAddress, int serverPort, String currentUsername) throws IOException {
        this.socket = new Socket(serverAddress, serverPort);
        this.out = new PrintWriter(socket.getOutputStream(), true); // Autoflush attivo
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.currentUsername = currentUsername;
    }

    public int insertLimitOrder(String side, int dim, Double price) {
        int orderId = orderIdCounter.getAndIncrement(); // Generazione ID univoco

        // Creazione del JSON per l'inserimento dell'ordine
        ConcurrentMap<String, Object> request = new ConcurrentHashMap<>();
        request.put("operation", "limitorder");

        ConcurrentHashMap<String, Object> values = new ConcurrentHashMap<>();
        values.put("username", currentUsername); // Usa l'username autenticato
        values.put("orderId", orderId);
        values.put("side", side);
        values.put("size", dim);
        values.put("price", price);

        request.put("values", values);
        String jsonRequest = gson.toJson(request);

        // Invio dei dati al server
        out.println(jsonRequest);
        System.out.println("JSON inviato al server: " + jsonRequest);

        return receiveResponse(orderId);
    }

    public int insertMarketOrder(String side, int dim) {
        int orderId = orderIdCounter.getAndIncrement();

        // Creazione del JSON per Market Order
        ConcurrentMap<String, Object> request = new ConcurrentHashMap<>();
        request.put("operation", "marketorder");

        ConcurrentHashMap<String, Object> values = new ConcurrentHashMap<>();
        values.put("username", currentUsername);
        values.put("orderId", orderId);
        values.put("side", side);
        values.put("size", dim);

        request.put("values", values);
        String jsonRequest = gson.toJson(request);

        // Invio dei dati al server
        out.println(jsonRequest);
        System.out.println("JSON inviato al server: " + jsonRequest);

        return receiveResponse(orderId);
    }

    public int insertStopOrder(String side, int dim, Double price) {
        int orderId = orderIdCounter.getAndIncrement();

        // Creazione del JSON per Stop Order
        ConcurrentMap<String, Object> request = new ConcurrentHashMap<>();
        request.put("operation", "stoporder");

        ConcurrentHashMap<String, Object> values = new ConcurrentHashMap<>();
        values.put("username", currentUsername);
        values.put("orderId", orderId);
        values.put("side", side);
        values.put("size", dim);
        values.put("stopPrice", price);

        request.put("values", values);
        String jsonRequest = gson.toJson(request);

        // Invio dei dati al server
        out.println(jsonRequest);
        System.out.println("JSON inviato al server: " + jsonRequest);

        return receiveResponse(orderId);
    }

    private int receiveResponse(int orderId) {
        try {
            String serverResponse = in.readLine();
            System.out.println("JSON ricevuto dal server: " + serverResponse);

            if (serverResponse == null) {
                System.out.println("Errore: risposta nulla dal server.");
                return -1;
            }

            // Decodifica la risposta JSON
            ConcurrentHashMap<String, Object> response = gson.fromJson(serverResponse, ConcurrentHashMap.class);
            double responseCode = (double) response.get("response");
            String errorMessage = (String) response.get("errorMessage");

            // Interpretazione della risposta
            if (responseCode == 100) {
                System.out.println("Ordine inserito con successo! ID: " + orderId);
                return orderId;
            } else {
                System.out.println("Errore: " + errorMessage);
                return -1;
            }
        } catch (IOException e) {
            System.out.println("Errore di comunicazione con il server.");
            e.printStackTrace();
            return -1;
        }
    }

    public void close() throws IOException {
        in.close();
        out.close();
        socket.close();
    }
}
