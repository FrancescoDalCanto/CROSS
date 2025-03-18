package Client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CROSSClient {
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private final Gson gson = new Gson();

    // Variabile per tener traccia dell'utente loggato
    private String currentUsername = null;

    public CROSSClient(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    /**
     * Metodo per la registrazione di un nuovo utente.
     */
    protected synchronized void handleRegister(Scanner scanner) {
        String username;
        String password;

        // Richiedi username e password con validazione
        do {
            System.out.print("Username: ");
            username = scanner.nextLine().trim();

            System.out.print("Password: ");
            password = scanner.nextLine().trim();

            if (username.isEmpty() || password.isEmpty()) {
                System.out.println("Username e Password non possono essere vuoti. Riprova.");
            } else {
                break;
            }
        } while (true);

        ConcurrentMap<String, Object> request = new ConcurrentHashMap<>();
        request.put("operation", "register");

        ConcurrentHashMap<String, String> values = new ConcurrentHashMap<>();
        values.put("username", username);
        values.put("password", password);

        request.put("values", values);
        String jsonRequest = gson.toJson(request);

        System.out.println("JSON inviato al server: " + jsonRequest);
        out.println(jsonRequest);

        try {
            String serverResponse = in.readLine();
            System.out.println("JSON ricevuto dal server: " + serverResponse);

            ConcurrentHashMap<String, Object> response = gson.fromJson(serverResponse, ConcurrentHashMap.class);
            double responseCode = (double) response.get("response");
            String errorMessage = (String) response.get("errorMessage");

            if (responseCode == 100) {
                System.out.println("Registrazione completata con successo!");
                currentUsername = username;
            } else {
                System.out.println("Errore: " + errorMessage);
            }
        } catch (IOException e) {
            System.out.println("Errore di comunicazione con il server.");
            e.printStackTrace();
        }
    }

    /**
     * Metodo per il login di un utente.
     */
    protected synchronized boolean handleLogin(Scanner scanner) {
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        ConcurrentMap<String, Object> request = new ConcurrentHashMap<>();
        request.put("operation", "login");

        ConcurrentHashMap<String, String> values = new ConcurrentHashMap<>();
        values.put("username", username);
        values.put("password", password);

        request.put("values", values);
        String jsonRequest = gson.toJson(request);

        System.out.println("JSON inviato al server: " + jsonRequest);
        out.println(jsonRequest);

        try {
            String serverResponse = in.readLine();
            System.out.println("JSON ricevuto dal server: " + serverResponse);

            ConcurrentHashMap<String, Object> response = gson.fromJson(serverResponse, ConcurrentHashMap.class);
            double responseCode = (double) response.get("response");
            String errorMessage = (String) response.get("errorMessage");

            if (responseCode == 100) {
                System.out.println("Login completato con successo!");
                currentUsername = username;
                return true;
            } else {
                System.out.println("Errore: " + errorMessage);
            }
        } catch (IOException e) {
            System.out.println("Errore di comunicazione con il server.");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Metodo per il logout di un utente.
     */
    protected synchronized boolean handleLogout(Scanner scanner) {
        if (currentUsername == null) {
            System.out.println("Nessun utente loggato.");
            return false;
        }

        ConcurrentHashMap<String, Object> request = new ConcurrentHashMap<>();
        request.put("operation", "logout");

        ConcurrentHashMap<String, Object> values = new ConcurrentHashMap<>();
        values.put("username", currentUsername);

        request.put("values", values);
        String jsonRequest = gson.toJson(request);

        System.out.println("JSON inviato al server: " + jsonRequest);
        out.println(jsonRequest);

        try {
            String serverResponse = in.readLine();
            System.out.println("JSON ricevuto dal server: " + serverResponse);

            ConcurrentHashMap<String, Object> response = gson.fromJson(serverResponse, ConcurrentHashMap.class);
            double responseCode = (double) response.get("response");
            String errorMessage = (String) response.get("errorMessage");

            if (responseCode == 100) {
                System.out.println("Logout effettuato con successo!");
                currentUsername = null;
                return false;
            } else {
                System.out.println("Errore: " + errorMessage);
            }
        } catch (IOException e) {
            System.out.println("Errore di comunicazione con il server.");
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Metodo per l'inserimento di un ordine, usando la connessione esistente.
     */
    protected synchronized void handleInsert(Scanner scanner) {
        if (currentUsername == null) {
            System.out.println("Effettua prima il login o la registrazione.");
            return;
        }

        System.out.print("Inserire il tipo (limit, market, stop): ");
        String type = scanner.nextLine().trim().toLowerCase();

        System.out.print("Inserisci la dimensione: ");
        int dim = scanner.nextInt();
        scanner.nextLine(); // Consuma il newline

        System.out.print("Inserisci il tipo (bid/ask): ");
        String side = scanner.nextLine().trim().toLowerCase();

        System.out.print("Inserisci il valore: ");
        Double limitPrice = scanner.nextDouble();
        scanner.nextLine(); // Consuma il newline

        // Costruzione della richiesta JSON
        ConcurrentMap<String, Object> request = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Object> values = new ConcurrentHashMap<>();
        int orderId = new Random().nextInt(100000); // Genera ID ordine casuale (o logica tua)

        values.put("username", currentUsername);
        values.put("orderId", orderId);
        values.put("side", side);
        values.put("size", dim);

        switch (type) {
            case "limit":
                request.put("operation", "limitorder");
                values.put("price", limitPrice);
                break;
            case "market":
                request.put("operation", "marketorder");
                break;
            case "stop":
                request.put("operation", "stoporder");
                values.put("stopPrice", limitPrice);
                break;
            default:
                System.out.println("Tipo di ordine sconosciuto.");
                return;
        }

        request.put("values", values);
        String jsonRequest = gson.toJson(request);

        // Invio
        out.println(jsonRequest);
        System.out.println("JSON inviato al server: " + jsonRequest);

        // Ricezione risposta
        try {
            String serverResponse = in.readLine();
            System.out.println("JSON ricevuto dal server: " + serverResponse);

            ConcurrentHashMap<String, Object> response = gson.fromJson(serverResponse, ConcurrentHashMap.class);
            double responseCode = (double) response.get("response");
            String errorMessage = (String) response.get("errorMessage");

            if (responseCode == 100) {
                System.out.println("Ordine inserito con successo! ID: " + orderId);
            } else {
                System.out.println("Errore: " + errorMessage);
            }
        } catch (IOException e) {
            System.out.println("Errore di comunicazione con il server.");
            e.printStackTrace();
        }
    }

    protected synchronized void handleDelete(Scanner scanner) {
        if (currentUsername == null) {
            System.out.println("Effettua prima il login o la registrazione.");
            return;
        }

        System.out.print("Inserisci l'ID dell'ordine da cancellare: ");
        String orderId = scanner.nextLine().trim();

        // Creazione della richiesta JSON per cancellare l'ordine
        ConcurrentHashMap<String, Object> request = new ConcurrentHashMap<>();
        request.put("operation", "deleteorder");

        ConcurrentHashMap<String, Object> values = new ConcurrentHashMap<>();
        values.put("username", currentUsername);
        values.put("orderId", orderId);

        request.put("values", values);
        String jsonRequest = gson.toJson(request);

        // Invio al server
        System.out.println("JSON inviato al server: " + jsonRequest);
        out.println(jsonRequest);

        try {
            String serverResponse = in.readLine();
            System.out.println("JSON ricevuto dal server: " + serverResponse);

            ConcurrentHashMap<String, Object> response = gson.fromJson(serverResponse, ConcurrentHashMap.class);
            double responseCode = (double) response.get("response");
            String errorMessage = (String) response.get("errorMessage");

            if (responseCode == 100) {
                System.out.println("Ordine cancellato con successo!");
            } else {
                System.out.println("Errore: " + errorMessage);
            }
        } catch (IOException e) {
            System.out.println("Errore di comunicazione con il server.");
            e.printStackTrace();
        }
    }

    protected synchronized void handleHistory(Scanner scanner) {
        if (currentUsername == null) {
            System.out.println("Effettua prima il login o la registrazione.");
            return;
        }

        System.out.print("Inserire la data (MMYYYY): ");
        String date = scanner.nextLine().trim();

        if (date.length() != 6) {
            System.out.println("La data deve essere in formato MMYYYY (6 cifre).");
            return;
        }

        // Creazione richiesta JSON per history
        Map<String, Object> request = new HashMap<>();
        request.put("operation", "history");

        Map<String, Object> values = new HashMap<>();
        values.put("date", date);
        values.put("username", currentUsername);

        request.put("values", values);
        String jsonRequest = gson.toJson(request);

        out.println(jsonRequest);
        System.out.println("JSON inviato al server: " + jsonRequest);

        try {
            String serverResponse = in.readLine();
            System.out.println("JSON ricevuto dal server: " + serverResponse);

            Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> response = gson.fromJson(serverResponse, mapType);

            if (response == null || !response.containsKey("response")) {
                System.out.println("Errore: risposta non valida dal server.");
                return;
            }

            double responseCode = (double) response.get("response");
            if (responseCode != 100) {
                System.out.println("Errore: " + response.get("errorMessage"));
                return;
            }

            String fileName = (String) response.get("file");
            File file = new File(fileName);

            if (!file.exists()) {
                System.out.println("Errore: il file storico non esiste.");
                return;
            }

            // Lettura file storico
            List<Map<String, String>> orders;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                orders = gson.fromJson(reader, new TypeToken<List<Map<String, String>>>() {}.getType());
            }

            if (orders != null && !orders.isEmpty()) {
                System.out.println("Storico ordini:");
                for (Map<String, String> order : orders) {
                    System.out.println("ID Ordine: " + order.get("orderID"));
                    System.out.println("Tipo: " + order.get("type"));
                    System.out.println("Side: " + order.get("side"));
                    System.out.println("Dimensione: " + order.get("size"));
                    System.out.println("Prezzo: " + order.get("price"));
                    System.out.println("Data: " + order.get("date"));
                    System.out.println("--------------------------------------");
                }
            } else {
                System.out.println("Nessun ordine trovato per la data specificata.");
            }

            // Cancella file locale
            if (file.delete()) {
                System.out.println("DEBUG CLIENT: File " + fileName + " eliminato dopo la lettura.");
            } else {
                System.out.println("Errore nella cancellazione del file.");
            }

        } catch (IOException e) {
            System.out.println("Errore di comunicazione con il server.");
            e.printStackTrace();
        }
    }

    protected synchronized void handleUpdate(Scanner scanner) {
        if (currentUsername == null) {
            System.out.println("Effettua prima il login o la registrazione.");
            return;
        }

        System.out.print("Inserire la password corrente: ");
        String currentPassword = scanner.nextLine().trim();

        System.out.print("Inserire la nuova password: ");
        String newPassword = scanner.nextLine().trim();

        if (newPassword.equals(currentPassword)) {
            System.out.println("Errore: la nuova password non può essere uguale a quella corrente.");
            return;
        }

        // Creazione richiesta JSON per aggiornare credenziali
        ConcurrentHashMap<String, Object> request = new ConcurrentHashMap<>();
        request.put("operation", "updateCredentials");

        ConcurrentHashMap<String, Object> values = new ConcurrentHashMap<>();
        values.put("username", currentUsername);
        values.put("currentPassword", currentPassword);
        values.put("newPassword", newPassword);

        request.put("values", values);
        String jsonRequest = gson.toJson(request);

        System.out.println("JSON inviato al server: " + jsonRequest);
        out.println(jsonRequest);

        try {
            String serverResponse = in.readLine();
            System.out.println("JSON ricevuto dal server: " + serverResponse);

            ConcurrentHashMap<String, Object> response = gson.fromJson(serverResponse, ConcurrentHashMap.class);
            double responseCode = (double) response.get("response");
            String errorMessage = (String) response.get("errorMessage");

            if (responseCode == 100) {
                System.out.println("Update completato con successo!");
            } else {
                System.out.println("Errore: " + errorMessage);
            }
        } catch (IOException e) {
            System.out.println("Errore di comunicazione con il server.");
            e.printStackTrace();
        }
    }

}
