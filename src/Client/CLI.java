package Client;

import com.google.gson.Gson;

import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.System.out;

public class CLI{
    // Codici colore ANSI per una visualizzazione colorata in console
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";

    private boolean logged = false;

    private final CROSSClient metodi;

    private final Gson gson = new Gson();

    public CLI(Socket socket) throws Exception {
        this.metodi = new CROSSClient(socket);
    }

    /**
     * Metodo per fornire l'interfaccia all'utente
     */
    public void run(){
        Scanner scanner = new Scanner(System.in);

        while (true) {
            // Stampa il menu CLI
            out.println(BLUE + "\n-------------------------------------------" + RESET);
            out.println(GREEN + "       Welcome to the CROSS Client CLI      " + RESET);
            out.println(BLUE + "-------------------------------------------" + RESET);
            out.println(CYAN + "Available Commands:" + RESET);
            out.println(YELLOW + "1. Register   - Register a new user" + RESET);
            out.println(YELLOW + "2. Login      - Login to your account" + RESET);
            out.println(YELLOW + "3. Logout     - Logout from your account" + RESET);
            out.println(YELLOW + "4. Insert     - Insert an order (limit, market, or stop)" + RESET);
            out.println(YELLOW + "5. Cancel     - Cancel an existing order" + RESET);
            out.println(YELLOW + "6. History    - View order history" + RESET);
            out.println(YELLOW + "7. Update     - Update Credentials" + RESET);
            out.println(YELLOW + "8. Exit       - Chiude la connessione" + RESET);
            out.print(CYAN + "Enter your command: " + RESET);

            String command = scanner.nextLine().trim().toLowerCase();

            try {
                // Gestione dei comandi inseriti dall'utente
                switch (command) {
                    case "register":
                        metodi.handleRegister(scanner);
                        break;

                    case "login":
                        if(logged == false)
                            logged = metodi.handleLogin(scanner);
                        else
                            System.out.println("Login already logged.");
                        break;

                    case "logout":
                        if(logged == true)
                            logged = metodi.handleLogout(scanner);
                        else
                            System.out.println("Prima devi fare il login.");
                        break;

                    case "insert":
                        metodi.handleInsert(scanner);
                        break;

                    case "cancel":
                        metodi.handleDelete(scanner);
                        break;

                    case "history":
                        metodi.handleHistory(scanner);
                        break;

                    case "update":
                        metodi.handleUpdate(scanner);
                        break;
                    case "exit":
                        exit();
                        break;

                    default:
                        out.println(RED + "Invalid command. Please try again." + RESET);
                }
            } catch (Exception e) {
                // In caso di eccezione, stampa l'errore
                out.println(RED + "Error: " + e.getMessage() + RESET);
            }
        }
    }

    /**
     * Questo metodo serve per chiudere il server, ma visto che ci potrebbero essere n client connessi
     * il Server smette di ricevere nuove connessioni e quando tutti i client chiedono la chiusura (o dopo x minuti di inattivitá) chiude il server
     */
    public void exit(){
        // Creazione del JSON per la chiusura del server
        ConcurrentMap<String, Object> request = new ConcurrentHashMap<>();
        request.put("operation", "exit");

        // Converto il JSON in una stringa
        String jsonRequest = gson.toJson(request);

        // Invio i dati al server
        System.out.println("JSON inviato al server: " + jsonRequest);
        out.println(jsonRequest);
    }
}
