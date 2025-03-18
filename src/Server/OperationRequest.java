package Server;

import java.util.Map;

public class OperationRequest {
    private String operation;
    private Map<String, Object> values;  // oppure un'altra struttura se hai requisiti specifici

    public String getOperation() {
        return operation;
    }

    public Map<String, Object> getValues() {
        return values;
    }
}
