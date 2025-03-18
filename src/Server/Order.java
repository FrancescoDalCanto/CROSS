package Server;

public class Order {
    private int orderId;
    private String userId;
    private String type;
    private String orderType;
    private int size;
    private int price;
    private int stopPrice;
    private long timestamp;

    // Costruttore vuoto per Gson
    public Order() {}

    // Costruttore completo per creare ordine singolo
    public Order(int orderId, String userId, String type, String orderType, int size, int price, int stopPrice, long timestamp) {
        this.orderId = orderId;
        this.userId = userId;
        this.type = type;
        this.orderType = orderType;
        this.size = size;
        this.price = price;
        this.stopPrice = stopPrice;
        this.timestamp = timestamp;
    }

    // Getter e Setter per orderId
    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    // Getter e Setter per userId
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // Getter e Setter per type (bid/ask)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    // Getter e Setter per orderType (limit, market, stop)
    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    // Getter e Setter per size
    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    // Getter e Setter per price
    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    // Getter e Setter per stopPrice
    public int getStopPrice() {
        return stopPrice;
    }

    public void setStopPrice(int stopPrice) {
        this.stopPrice = stopPrice;
    }

    // Getter e Setter per timestamp
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // Ottieni l'ID del compratore (solo per ordini bid)
    public Integer getBuyerId() {
        if ("bid".equalsIgnoreCase(this.type)) {
            try {
                return Integer.parseInt(this.userId);
            } catch (NumberFormatException e) {
                System.err.println("⚠️ Errore parsing Buyer ID: " + this.userId);
                return null;
            }
        }
        return null;
    }

    // Ottieni l'ID del venditore (solo per ordini ask)
    public Integer getSellerId() {
        if ("ask".equalsIgnoreCase(this.type)) {
            try {
                return Integer.parseInt(this.userId);
            } catch (NumberFormatException e) {
                System.err.println("⚠️ Errore parsing Seller ID: " + this.userId);
                return null;
            }
        }
        return null;
    }
}
