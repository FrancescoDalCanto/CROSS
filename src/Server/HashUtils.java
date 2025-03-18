package Server;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/*
 * Classe singleton per l'hashing delle password
 * Contiene metodi per generare il sale (salt) e per calcolare l'hash SHA-256 di una password
 */
public class HashUtils {

    // Generatore di numeri pseudo-casuali per il sale (salt)
    private static final SecureRandom random = new SecureRandom();

    // Lunghezza dell'hash, che sarà la stessa di un hash SHA-256 in formato esadecimale
    public static final int HASH_LEN = 64;  // Lunghezza dell'hash come stringa esadecimale (SHA-256 è di 256 bit, che corrisponde a 64 caratteri esadecimali)

    /**
     * Genera un "sale" (salt) casuale di una lunghezza specificata.
     * Il sale viene utilizzato per rendere l'hash più sicuro, evitando che password identiche abbiano lo stesso hash.
     *
     * @param length la lunghezza del sale da generare
     * @return una stringa Base64 che rappresenta il sale generato
     */
    public static String generateSalt(int length) {
        byte[] salt = new byte[length];  // Crea un array di byte di dimensione "length"
        random.nextBytes(salt);  // Riempi l'array con numeri casuali
        return Base64.getEncoder().encodeToString(salt);  // Converte i byte in una stringa Base64 per un formato leggibile
    }

    /*
     * Esegue l'hashing di una stringa con SHA-256, combinando la stringa con un "sale" (salt) per aumentarne la sicurezza.
     *
     * @param input la stringa (password) da hashare
     * @param salt il sale da aggiungere alla stringa per rendere l'hash unico
     * @return la stringa esadecimale dell'hash SHA-256
     */
    public static String computeSHA256Hash(String input, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");  // Ottiene un'istanza dell'algoritmo SHA-256
            byte[] inputBytes = (input + salt).getBytes();  // Combina la stringa con il sale e la converte in un array di byte
            byte[] hashBytes = digest.digest(inputBytes);  // Calcola l'hash dei byte combinati (password + salt)
            return bytesToHex(hashBytes);  // Converte l'array di byte dell'hash in una stringa esadecimale
        } catch (NoSuchAlgorithmException e) {  // Gestisce il caso in cui l'algoritmo SHA-256 non fosse disponibile
            throw new RuntimeException("Algoritmo SHA-256 non trovato", e);
        }
    }

    /*
     * Converte un array di byte in una stringa esadecimale.
     * Utilizzata per trasformare l'hash (in byte) in una rappresentazione leggibile.
     *
     * @param bytes l'array di byte da convertire
     * @return la stringa esadecimale corrispondente
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder Hex = new StringBuilder();  // StringBuilder per costruire la stringa esadecimale
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);  // Converte ogni byte in esadecimale
            if (hex.length() == 1) {  // Aggiunge uno zero iniziale se necessario (per garantire due caratteri per byte)
                Hex.append('0');
            }
            Hex.append(hex);  // Aggiunge il valore esadecimale alla stringa finale
        }
        return Hex.toString();  // Restituisce la stringa esadecimale
    }
}
