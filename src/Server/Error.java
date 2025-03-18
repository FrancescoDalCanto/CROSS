package Server;

public class Error {

    // Errore per la registrazione:
    // 100 - OK
    // 101 - invalid password
    // 102 - username not available
    // 103 - other error cases
    public static String getRegistrationErrorResponse(int code) {
        String message;
        switch(code) {
            case 100: message = "OK"; break;
            case 101: message = "Invalid password"; break;
            case 102: message = "Username not available"; break;
            case 103: message = "Other error cases"; break;
            default:  message = "Unknown error"; break;
        }
        return "{\"response\": " + code + ", \"errorMessage\": \"" + message + "\"}";
    }

    // Errore per l'update delle credenziali:
    // 100 - OK
    // 101 - invalid new password
    // 102 - username/old_password mismatch or non existent username
    // 103 - new password equal to old one
    // 104 - user currently logged in
    // 105 - other error cases
    public static String getUpdateCredentialsErrorResponse(int code) {
        String message;
        switch(code) {
            case 100: message = "OK"; break;
            case 101: message = "Invalid new password"; break;
            case 102: message = "Username/old_password mismatch or non existent username"; break;
            case 103: message = "New password equal to old one"; break;
            case 104: message = "User currently logged in"; break;
            case 105: message = "Other error cases"; break;
            default:  message = "Unknown error"; break;
        }
        return "{\"response\": " + code + ", \"errorMessage\": \"" + message + "\"}";
    }

    // Errore per il login:
    // 100 - OK
    // 101 - username/password mismatch or non existent username
    // 102 - user already logged in
    // 103 - other error cases
    public static String getLoginErrorResponse(int code) {
        String message;
        switch(code) {
            case 100: message = "OK"; break;
            case 101: message = "Username/password mismatch or non existent username"; break;
            case 102: message = "User already logged in"; break;
            case 103: message = "Other error cases"; break;
            default:  message = "Unknown error"; break;
        }
        return "{\"response\": " + code + ", \"errorMessage\": \"" + message + "\"}";
    }

    // Errore per il logout:
    // 100 - OK
    // 101 - user not logged in or other error cases
    public static String getLogoutErrorResponse(int code) {
        String message;
        switch(code) {
            case 100: message = "OK"; break;
            case 101: message = "User not logged in or other error cases"; break;
            default:  message = "Unknown error"; break;
        }
        return "{\"response\": " + code + ", \"errorMessage\": \"" + message + "\"}";
    }

    // Errore per la cancellazione di un ordine:
    // 100 - OK
    // 101 - order does not exist or belongs to different user or has already been finalized or other error cases
    public static String getCancelOrderErrorResponse(int code) {
        String message;
        switch(code) {
            case 100: message = "OK"; break;
            case 101: message = "Order does not exist or belongs to different user or has already been finalized or other error cases"; break;
            default:  message = "Unknown error"; break;
        }
        return "{\"response\": " + code + ", \"errorMessage\": \"" + message + "\"}";
    }

    // Metodo per gestire errori di Bad Request (codice 400)
    public static String getBadRequestResponse() {
        return "{\"response\": 400, \"errorMessage\": \"Bad Request: Operazione non riconosciuta o richiesta mal formata\"}";
    }

    // Metodo generico per errori HTTP 401 - Unauthorized
    public static String getUnauthorizedResponse() {
        return "{\"response\": 401, \"errorMessage\": \"Unauthorized: User not authenticated\"}";
    }

    // Metodo generico per errori HTTP 500 - Internal Server Error
    public static String getInternalServerErrorResponse() {
        return "{\"response\": 500, \"errorMessage\": \"Internal Server Error\"}";
    }

    // Metodo generico per errori HTTP 501 - Not Implemented
    public static String getNotImplementedResponse() {
        return "{\"response\": 501, \"errorMessage\": \"Service Not Implemented\"}";
    }

    // Metodo generico per errori HTTP 404 - User not found
    public static String getNotUser() {
        return "{ \"response\": 404, \"errorMessage\": \"User not found\" }";
    }
}
