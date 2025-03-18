package Client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

public class UserMenager {
    private static final String user = "Users.json";
    private Set<String> utentiRegistrati;

    public UserMenager() {
        this.utentiRegistrati = leggiUtentiDaJson();
    }

    /**
     * legge gli username dal file JSON e li salva dentro al Set
     * @return
     */
    private Set<String> leggiUtentiDaJson() {
        try (FileReader reader = new FileReader(user)) {
            Type tipoSet = new TypeToken<HashSet<String>>() {}.getType();
            return new Gson().fromJson(reader, tipoSet);
        } catch (IOException e) {
            System.out.println("Errore nella lettura del file. File inesistente o vuoto.");
            return new HashSet<>();
        }
    }

    /**
     * Controlla se un username esiste già
     */
    public boolean isUsernameTaken(String username) {
        return utentiRegistrati.contains(username);
    }
}
