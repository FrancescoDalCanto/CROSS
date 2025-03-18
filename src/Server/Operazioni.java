package Server;

import CommonClass.CompraVendita;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.List;


import java.time.LocalDate;


public class Operazioni {

    // File degli utenti e lock per la sincronizzazione
    private static final File usersPath = new File("src/Document/Users.json");
    private static final Object usersLock = new Object();

    // File degli ordini e lock per la sincronizzazione
    private static final File ordersPath = new File("src/Document/Orders.json");
    private static final Object ordersLock = new Object();

    // Metodi per collegare l'ordine singolo ad un ID utente
    private static final File userOrderPath = new File("src/Document/UserOrder.json");
    private static final Object userOrderLock = new Object();

    /**
     * Registra utente dentro al file Json
     * @param values
     * @param out
     */
    public static void Register(Map<String, Object> values, PrintWriter out) {
        String username = (String) values.get("username");
        String password = (String) values.get("password");

        // Genera un salt e calcola l'hash della password
        String salt = HashUtils.generateSalt(16);
        String passwordHash = HashUtils.computeSHA256Hash(password, salt);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<Map<String, String>> users;

        synchronized (usersLock) {
            // Legge la lista degli utenti dal file
            if (usersPath.exists()) {
                try (FileReader reader = new FileReader(usersPath)) {
                    Type listType = new TypeToken<List<Map<String, String>>>() {}.getType();
                    users = gson.fromJson(reader, listType);
                    if (users == null) {
                        users = new ArrayList<>();
                    }
                } catch (IOException e) {
                    String errorResponse = Error.getRegistrationErrorResponse(103);
                    System.out.println("Risposta inviata al client: " + errorResponse);
                    out.println(errorResponse);
                    out.flush();
                    return;
                }
            } else {
                // Se il file non esiste, restituisce un errore
                String errorResponse = Error.getRegistrationErrorResponse(103);
                System.out.println("Risposta inviata al client: " + errorResponse);
                out.println(errorResponse);
                out.flush();
                return;
            }

            // Controlla se l'username è già presente (case-insensitive)
            for (Map<String, String> user : users) {
                if (user.get("username") != null && user.get("username").equalsIgnoreCase(username)) {
                    String errorResponse = Error.getRegistrationErrorResponse(102);
                    System.out.println("Risposta inviata al client: " + errorResponse);
                    out.println(errorResponse);
                    out.flush();
                    return;
                }
            }

            // Crea il nuovo utente e lo aggiunge
            Map<String, String> newUser = new HashMap<>();
            newUser.put("username", username);
            newUser.put("passwordHash", passwordHash);
            newUser.put("salt", salt);
            users.add(newUser);

            // Scrive la lista aggiornata nel file JSON
            try (FileWriter writer = new FileWriter(usersPath)) {
                gson.toJson(users, writer);
            } catch (IOException e) {
                String errorResponse = Error.getRegistrationErrorResponse(103);
                System.out.println("Risposta inviata al client: " + errorResponse);
                out.println(errorResponse);
                out.flush();
                return;
            }
        }

        // Restituisce OK (codice 100)
        String successResponse = Error.getRegistrationErrorResponse(100);
        System.out.println("Risposta inviata al client: " + successResponse);
        out.println(successResponse);
        out.flush();
    }


    /**
     * Esegue il Login di un utente
     * @param values
     * @param out
     * @return
     */
    public static boolean Login(Map<String, Object> values, PrintWriter out) {
        String username = (String) values.get("username");
        String password = (String) values.get("password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            out.println(Error.getRegistrationErrorResponse(10));
            return false;
        }

        if (SessionManager.isLoggedIn(username)) {
            out.println(Error.getRegistrationErrorResponse(102));
            return false;
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<Map<String, String>> users;

        try (Reader reader = new FileReader(usersPath)) {
            Type listType = new TypeToken<List<Map<String, String>>>() {}.getType();
            users = gson.fromJson(reader, listType);
            if (users == null) {
                users = new ArrayList<>();
            }
        } catch (IOException e) {
            System.err.println("Errore nel caricamento degli utenti: " + e.getMessage());
            out.println(Error.getInternalServerErrorResponse());
            return false;
        }

        // Cerca l'utente nel JSON
        for (Map<String, String> user : users) {
            if (user.get("username").equals(username)) {
                String storedSalt = user.get("salt");
                String storedHash = user.get("passwordHash");

                // Hash della password inserita con il salt memorizzato
                String computedHash = HashUtils.computeSHA256Hash(password, storedSalt);

                if (computedHash.equals(storedHash)) {
                    // Login riuscito, aggiungere la sessione
                    SessionManager.addSession(username, new Session(username, null));
                    out.println(Error.getRegistrationErrorResponse(100));
                    return true;
                }
            }
        }

        // Se nessuna corrispondenza trovata
        out.println(Error.getRegistrationErrorResponse(101));
        return false;
    }

    /**
     *
     * @param values
     * @param out
     */
    public static void LimitOrder(Map<String, Object> values, PrintWriter out, CompraVendita compraVendita) {
        // Estrazione dei valori dalla richiesta
        String username = (String) values.get("username");
        String side = (String) values.get("side");
        int size = ((Number) values.get("size")).intValue();
        int orderId = ((Number) values.get("orderId")).intValue();
        float price = ((Number) values.get("price")).floatValue();

        // Ottieni la data corrente in formato MMYYYY
        LocalDate currentDate = LocalDate.now();
        String date = String.format("%02d", currentDate.getMonthValue()) + currentDate.getYear();

        System.out.println("📌 Data formattata: " + date);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<Map<String, String>> orders = new ArrayList<>();

        Order order;
        synchronized (ordersLock) {
            if (ordersPath.exists()) {
                try (FileReader reader = new FileReader(ordersPath)) {
                    Type listType = new TypeToken<List<Map<String, String>>>() {
                    }.getType();
                    orders = gson.fromJson(reader, listType);
                    if (orders == null) {
                        orders = new ArrayList<>();
                    }
                } catch (IOException e) {
                    System.err.println("❌ Errore nella lettura del file JSON: " + e.getMessage());
                    out.println(Error.getRegistrationErrorResponse(103));
                    return;
                }
            } else {
                System.err.println("⚠️ Il file degli ordini non esiste.");
                out.println(Error.getRegistrationErrorResponse(103));
                return;
            }

            // **Creazione dell'ordine**
            order = new Order();
            order.setOrderId(orderId);
            order.setUserId(username); // Se username è l'ID utente
            order.setType(side);
            order.setOrderType("limit");
            order.setSize(size);
            order.setPrice((int) price); // Convertito in int per evitare problemi

            // **Salva l'ordine nel JSON**
            Map<String, String> newOrder = new HashMap<>();
            newOrder.put("username", username);
            newOrder.put("type", "Limit");
            newOrder.put("side", side);
            newOrder.put("size", String.valueOf(size));
            newOrder.put("price", String.valueOf(price));
            newOrder.put("orderID", String.valueOf(orderId));
            newOrder.put("date", date);
            orders.add(newOrder);

            try (FileWriter writer = new FileWriter(ordersPath)) {
                gson.toJson(orders, writer);
                System.out.println("✅ Ordine salvato con successo.");
            } catch (IOException e) {
                System.err.println("❌ Errore nella scrittura del file JSON: " + e.getMessage());
                out.println(Error.getRegistrationErrorResponse(103));
                return;
            }
        }

        // **Aggiunta dell'ordine in CompraVendita e Matching**
        Set<Integer> usersToNotify = compraVendita.addOrder(order);
        System.out.println("📢 Utenti da notificare: " + usersToNotify);

        // Collegamento aggiornato username e orderID
        updateUserOrderMapping(username, orderId);

        // **Invio della Notifica UDP agli utenti coinvolti**
        if (!usersToNotify.isEmpty()) {
            compraVendita.MessageUDP(usersToNotify);
        }

        // **Risposta di successo**
        out.println(Error.getRegistrationErrorResponse(100));
    }


    /**
     *
     * @param values
     * @param out
     */
    public static void MarketOrder(Map<String, Object> values, PrintWriter out, CompraVendita compraVendita) {
        String username = (String) values.get("username");
        int orderId = ((Number) values.get("orderId")).intValue();
        String side = (String) values.get("side");
        int size = ((Number) values.get("size")).intValue();

        // Data corrente in formato MMYYYY
        LocalDate currentDate = LocalDate.now();
        String date = String.format("%02d", currentDate.getMonthValue()) + currentDate.getYear();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<Map<String, String>> orders = new ArrayList<>();

        synchronized (ordersLock) {
            if (ordersPath.exists()) {
                try (FileReader reader = new FileReader(ordersPath)) {
                    Type listType = new TypeToken<List<Map<String, String>>>() {}.getType();
                    orders = gson.fromJson(reader, listType);
                    if (orders == null) {
                        orders = new ArrayList<>();
                    }
                } catch (IOException e) {
                    System.err.println("Errore nella lettura del file JSON: " + e.getMessage());
                    out.println(Error.getRegistrationErrorResponse(103));
                    return;
                }
            } else {
                out.println(Error.getRegistrationErrorResponse(103));
                return;
            }

            // Salvataggio anche sul file JSON
            Map<String, String> newOrder = new HashMap<>();
            newOrder.put("username", username);
            newOrder.put("type", "market");
            newOrder.put("side", side);
            newOrder.put("size", String.valueOf(size));
            newOrder.put("orderID", String.valueOf(orderId));
            newOrder.put("date", date);
            orders.add(newOrder);

            try (FileWriter writer = new FileWriter(ordersPath)) {
                gson.toJson(orders, writer);
                System.out.println("Ordine salvato con successo.");
            } catch (IOException e) {
                System.err.println("Errore nella scrittura del file JSON: " + e.getMessage());
                out.println(Error.getRegistrationErrorResponse(103));
                return;
            }
        }

        // **CREAZIONE E INSERIMENTO NEL BOOK DI COMPRAVENDITA**
        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(username);
        order.setType(side);
        order.setOrderType("market");
        order.setSize(size);

        Set<Integer> usersToNotify = compraVendita.addOrder(order);
        System.out.println("📢 Utenti da notificare: " + usersToNotify);

        // Collegamento aggiornato username e orderID
        updateUserOrderMapping(username, orderId);

        if (!usersToNotify.isEmpty()) {
            compraVendita.MessageUDP(usersToNotify);
        }

        out.println(Error.getRegistrationErrorResponse(100));
    }


    /**
     *
     * @param values
     * @param out
     */
    public static void StopOrder(Map<String, Object> values, PrintWriter out, CompraVendita compraVendita) {
        String username = (String) values.get("username");
        String side = (String) values.get("side");
        int size = ((Number) values.get("size")).intValue();
        int orderId = ((Number) values.get("orderId")).intValue();
        float stopPrice = ((Number) values.get("stopPrice")).floatValue();

        // Data corrente in formato MMYYYY
        LocalDate currentDate = LocalDate.now();
        String date = String.format("%02d", currentDate.getMonthValue()) + currentDate.getYear();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<Map<String, String>> orders = new ArrayList<>();

        synchronized (ordersLock) {
            if (ordersPath.exists()) {
                try (FileReader reader = new FileReader(ordersPath)) {
                    Type listType = new TypeToken<List<Map<String, String>>>() {}.getType();
                    orders = gson.fromJson(reader, listType);
                    if (orders == null) {
                        orders = new ArrayList<>();
                    }
                } catch (IOException e) {
                    System.err.println("Errore nella lettura del file JSON: " + e.getMessage());
                    out.println(Error.getRegistrationErrorResponse(103));
                    return;
                }
            } else {
                out.println(Error.getRegistrationErrorResponse(103));
                return;
            }

            // Salvataggio anche nel file JSON
            Map<String, String> newOrder = new HashMap<>();
            newOrder.put("username", username);
            newOrder.put("type", "stop");
            newOrder.put("side", side);
            newOrder.put("size", String.valueOf(size));
            newOrder.put("price", String.valueOf(stopPrice));
            newOrder.put("orderID", String.valueOf(orderId));
            newOrder.put("date", date);
            orders.add(newOrder);

            try (FileWriter writer = new FileWriter(ordersPath)) {
                gson.toJson(orders, writer);
                System.out.println("Ordine salvato con successo.");
            } catch (IOException e) {
                System.err.println("Errore nella scrittura del file JSON: " + e.getMessage());
                out.println(Error.getRegistrationErrorResponse(103));
                return;
            }
        }

        // **CREAZIONE E INSERIMENTO NEL BOOK DI COMPRAVENDITA**
        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(username);
        order.setType(side);
        order.setOrderType("stop");
        order.setSize(size);
        order.setStopPrice((int) stopPrice); // Convertito in int per compatibilità

        Set<Integer> usersToNotify = compraVendita.addOrder(order);
        System.out.println("📢 Utenti da notificare: " + usersToNotify);

        // Collegamento aggiornato username e orderID
        updateUserOrderMapping(username, orderId);

        if (!usersToNotify.isEmpty()) {
            compraVendita.MessageUDP(usersToNotify);
        }

        out.println(Error.getRegistrationErrorResponse(100));
    }

    /**
     *
     * @param values Mappa contenente i parametri della richiesta, incluso il mese nel formato MMYYYY.
     * @param out PrintWriter per inviare la risposta al client.
     */
    public static void History(Map<String, Object> values, PrintWriter out, CompraVendita compraVendita) {
        /* Leggo dal file di associazione tutti gli ordini relativi all'utente */


        if (values == null) {
            out.println("{ \"response\": 400, \"errorMessage\": \"Dati di richiesta mancanti\" }");
            return;
        }

        String month = (String) values.get("date");
        String username = (String) values.get("username"); // Aggiunto il recupero dell'username

        if (month == null || !month.matches("\\d{6}")) {
            out.println("{ \"response\": 400, \"errorMessage\": \"Formato mese non valido. Usa MMYYYY.\" }");
            return;
        }

        if (username == null || username.isEmpty()) {
            out.println("{ \"response\": 400, \"errorMessage\": \"Username mancante.\" }");
            return;
        }

        System.out.println("DEBUG: Mese ricevuto -> " + month);
        System.out.println("DEBUG: Username ricevuto -> " + username);

        List<Map<String, Object>> filteredOrders = new ArrayList<>();

        synchronized (ordersLock) {
            File file = new File(String.valueOf(ordersPath));
            if (!file.exists() || !file.canRead()) {
                out.println("{ \"response\": 500, \"errorMessage\": \"File degli ordini non trovato o non accessibile.\" }");
                return;
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            List<Map<String, Object>> completedOrders;

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                completedOrders = gson.fromJson(br, new TypeToken<List<Map<String, Object>>>() {}.getType());
                if (completedOrders == null) completedOrders = new ArrayList<>();
            } catch (IOException e) {
                e.printStackTrace();
                out.println("{ \"response\": 500, \"errorMessage\": \"Errore nel recupero degli ordini.\" }");
                return;
            }

            // Filtra gli ordini per data e username
            for (Map<String, Object> order : completedOrders) {
                String orderDate = (String) order.get("date");
                String orderUser = (String) order.get("username"); // Supponiamo che gli ordini abbiano il campo "username"

                if (orderDate != null && orderDate.equals(month) && orderUser != null && orderUser.equals(username)) {
                    filteredOrders.add(order);
                }
            }
        }

        // Scrittura dei risultati in un file per il client
        String clientFileName = "history_" + username + ".json";
        File clientFile = new File(clientFileName);

        try (FileWriter writer = new FileWriter(clientFile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(filteredOrders, writer);
            System.out.println("DEBUG SERVER: File JSON creato -> " + clientFileName);
        } catch (IOException e) {
            e.printStackTrace();
            out.println("{ \"response\": 500, \"errorMessage\": \"Errore nella scrittura del file.\" }");
            return;
        }

        // Avvisa il client che il file è pronto
        out.println("{ \"response\": 100, \"file\": \"" + clientFileName + "\" }");
        out.flush();
    }

    /**
     * Cancella un ordine specificato da un utente.
     * @param values I parametri della richiesta, che contengono l'ID dell'utente e l'ID dell'ordine da cancellare.
     * @param out Il PrintWriter per inviare la risposta al client.
     * @param compraVendita Un'istanza della classe CompraVendita per la gestione degli ordini.
     */
    public static void CancelOrder(Map<String, Object> values, PrintWriter out, CompraVendita compraVendita) {
        // Estrai i parametri dalla richiesta
        String username = (String) values.get("username");
        int orderId = ((Number) values.get("orderId")).intValue();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<Map<String, String>> orders = new ArrayList<>();
        boolean orderFound = false;

        synchronized (ordersLock) {
            if (ordersPath.exists()) {
                try (FileReader reader = new FileReader(ordersPath)) {
                    Type listType = new TypeToken<List<Map<String, String>>>() {}.getType();
                    orders = gson.fromJson(reader, listType);
                    if (orders == null) {
                        orders = new ArrayList<>();
                    }
                } catch (IOException e) {
                    System.err.println("Errore nella lettura del file JSON: " + e.getMessage());
                    out.println(Error.getRegistrationErrorResponse(103));
                    return;
                }
            } else {
                System.err.println("Il file degli ordini non esiste.");
                out.println(Error.getRegistrationErrorResponse(103));
                return;
            }

            // Cerca e rimuovi l'ordine
            Iterator<Map<String, String>> iterator = orders.iterator();
            while (iterator.hasNext()) {
                Map<String, String> order = iterator.next();
                String orderUser = order.get("username");
                int orderIdFromFile = Integer.parseInt(order.get("orderID"));

                // Controlla se l'ordine appartiene all'utente e ha lo stesso ID
                if (orderUser != null && orderUser.equals(username) && orderIdFromFile == orderId) {
                    iterator.remove();  // Rimuovi l'ordine
                    orderFound = true;
                    break;
                }
            }

            if (orderFound) {
                // Riscrivi il file con la lista aggiornata degli ordini
                try (FileWriter writer = new FileWriter(ordersPath)) {
                    gson.toJson(orders, writer);
                    System.out.println("Ordine cancellato con successo.");
                } catch (IOException e) {
                    System.err.println("Errore nella scrittura del file JSON: " + e.getMessage());
                    out.println(Error.getRegistrationErrorResponse(103));
                    return;
                }
            } else {
                // Se l'ordine non è stato trovato
                out.println("{ \"response\": 404, \"errorMessage\": \"Ordine non trovato.\" }");
                return;
            }
        }

        // Invia risposta di successo
        out.println("{ \"response\": 100, \"message\": \"Ordine cancellato con successo.\" }");
    }


    /**
     * @param values
     * @param out
     */
    public static void UpdateCredentials(Map<String, Object> values, PrintWriter out) {
        String username = (String) values.get("username");
        String current_password = (String) values.get("currentPassword");
        String new_password = (String) values.get("newPassword");

        if (username == null || current_password == null || new_password == null ||
                username.isEmpty() || current_password.isEmpty() || new_password.isEmpty()) {
            out.println(Error.getBadRequestResponse());
            return;
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<Map<String, String>> users;

        synchronized (usersLock) {
            // Caricamento degli utenti dal file JSON
            try (Reader reader = new FileReader(usersPath)) {
                Type listType = new TypeToken<List<Map<String, String>>>() {
                }.getType();
                users = gson.fromJson(reader, listType);
                if (users == null) {
                    users = new ArrayList<>();
                }
            } catch (IOException e) {
                System.err.println("Errore nella lettura del file utenti: " + e.getMessage());
                out.println(Error.getInternalServerErrorResponse());
                return;
            }

            // Cerca l'utente nel JSON
            boolean userFound = false;
            for (Map<String, String> user : users) {
                if (user.get("username").equals(username)) {
                    userFound = true;
                    String storedSalt = user.get("salt");
                    String storedHash = user.get("passwordHash");

                    // Verifica la password attuale
                    String computedHash = HashUtils.computeSHA256Hash(current_password, storedSalt);
                    if (!computedHash.equals(storedHash)) {
                        out.println(Error.getUnauthorizedResponse());
                        return;
                    }

                    // Genera un nuovo salt e calcola l'hash della nuova password
                    String newSalt = HashUtils.generateSalt(16);
                    String newPasswordHash = HashUtils.computeSHA256Hash(new_password, newSalt);

                    // Aggiorna i dati dell'utente
                    user.put("passwordHash", newPasswordHash);
                    user.put("salt", newSalt);

                    break;
                }
            }

            if (!userFound) {
                out.println(Error.getNotUser());
                return;
            }

            // Salva il file aggiornato
            try (Writer writer = new FileWriter(usersPath)) {
                gson.toJson(users, writer);
            } catch (IOException e) {
                System.err.println("Errore nella scrittura del file utenti: " + e.getMessage());
                out.println(Error.getInternalServerErrorResponse());
                return;
            }
        }
        out.println(Error.getRegistrationErrorResponse(100));
    }



    // Mappa che collega ogni ordine di un cliente ad un ID
    public static void updateUserOrderMapping(String username, int orderId) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Map<String, List<Integer>> userOrders;

        synchronized (userOrderLock) {
            // Leggi il file
            if (userOrderPath.exists()) {
                try (FileReader reader = new FileReader(userOrderPath)) {
                    Type mapType = new TypeToken<Map<String, List<Integer>>>() {}.getType();
                    userOrders = gson.fromJson(reader, mapType);
                    if (userOrders == null) {
                        userOrders = new HashMap<>();
                    }
                } catch (IOException e) {
                    System.err.println("Errore lettura UserOrder.json: " + e.getMessage());
                    userOrders = new HashMap<>();
                }
            } else {
                userOrders = new HashMap<>();
            }

            // Aggiungi l'ordine all'utente
            userOrders.computeIfAbsent(username, k -> new ArrayList<>()).add(orderId);

            // Scrivi di nuovo il file
            try (FileWriter writer = new FileWriter(userOrderPath)) {
                gson.toJson(userOrders, writer);
            } catch (IOException e) {
                System.err.println("Errore scrittura UserOrder.json: " + e.getMessage());
            }
        }
    }

}
