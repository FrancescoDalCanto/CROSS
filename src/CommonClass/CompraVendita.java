package CommonClass;

import Server.Order;
import com.google.gson.*;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CompraVendita {

    private static final String ORDER_FILE = "src/Document/Orders.json";
    private static final String COMPLETED_ORDERS_FILE = "src/Document/CompletedOrders.json";
    private static final Gson gson = new Gson();

    private static final ConcurrentHashMap<Integer, Order> ordersMap = new ConcurrentHashMap<>();
    private static PriorityQueue<Order> bidOrders = new PriorityQueue<>(new BidComparator());
    private static PriorityQueue<Order> askOrders = new PriorityQueue<>(new AskComparator());
    private static List<Order> stopOrders = new ArrayList<>(); // Lista per gli Stop Orders

    private static final ConcurrentHashMap<Integer, ClientInfo> clients = new ConcurrentHashMap<>();

    /* Inizializza il book degli ordini e processa quelli esistenti */
    public Set<Integer> initializeOrderBook() throws Exception {
        loadOrders();
        processStopOrders();  // Attiva eventuali stop orders
        return matching();    // Esegue il matching iniziale
    }

    /* Carica gli ordini dal file JSON */
    private static void loadOrders() throws Exception {
        if (Files.exists(Paths.get(ORDER_FILE))) {
            try (FileReader reader = new FileReader(ORDER_FILE)) {
                JsonArray jsonArray = JsonParser.parseReader(reader).getAsJsonArray();
                for (JsonElement jsonElement : jsonArray) {
                    Order order = gson.fromJson(jsonElement, Order.class);
                    ordersMap.put(order.getOrderId(), order);

                    switch (order.getOrderType()) {
                        case "limit":
                            if ("bid".equals(order.getType())) {
                                bidOrders.add(order);
                            } else {
                                askOrders.add(order);
                            }
                            break;
                        case "stop":
                            stopOrders.add(order);
                            break;
                    }
                }
            }
        }
    }

    /* Aggiunge un nuovo ordine, lo persiste e ricalcola il matching */
    public Set<Integer> addOrder(Order order) {
        ordersMap.put(order.getOrderId(), order);

        switch (order.getOrderType()) {
            case "market":
                executeMarketOrder(order);
                break;
            case "limit":
                if (order.getType().equals("bid")) {
                    bidOrders.add(order);
                } else {
                    askOrders.add(order);
                }
                break;
            case "stop":
                stopOrders.add(order);
                break;
        }

        updateOrdersFile();
        processStopOrders();
        // Richiamo matching, ovvero ricontrollo la situazione rispetto alla precedente
        return matching();
    }

    /* Processa Stop Orders: attiva quelli che hanno raggiunto il trigger */
    private void processStopOrders() {
        Iterator<Order> iterator = stopOrders.iterator();
        while (iterator.hasNext()) {
            Order stopOrder = iterator.next();
            if ((stopOrder.getType().equals("bid") && stopOrder.getStopPrice() <= getBestAskPrice()) ||
                    (stopOrder.getType().equals("ask") && stopOrder.getStopPrice() >= getBestBidPrice())) {
                stopOrder.setOrderType("market");
                executeMarketOrder(stopOrder);
                iterator.remove();
            }
        }
    }

    /* Esegue un Market Order immediatamente */
    private void executeMarketOrder(Order order) {
        if (order.getType().equals("bid")) {
            while (!askOrders.isEmpty() && order.getSize() > 0) {
                Order bestAsk = askOrders.peek();
                int tradeSize = Math.min(order.getSize(), bestAsk.getSize());
                executeTrade(order, bestAsk, tradeSize);
                order.setSize(order.getSize() - tradeSize);
                bestAsk.setSize(bestAsk.getSize() - tradeSize);
                if (bestAsk.getSize() == 0) askOrders.poll();
            }
        } else if (order.getType().equals("ask")) {
            while (!bidOrders.isEmpty() && order.getSize() > 0) {
                Order bestBid = bidOrders.peek();
                int tradeSize = Math.min(order.getSize(), bestBid.getSize());
                executeTrade(bestBid, order, tradeSize);
                order.setSize(order.getSize() - tradeSize);
                bestBid.setSize(bestBid.getSize() - tradeSize);
                if (bestBid.getSize() == 0) bidOrders.poll();
            }
        }
    }

    /* Esegue il matching per gli ordini Limit */
    public Set<Integer> matching() {
        Set<Integer> ordersToNotify = new HashSet<>(); // Contiene tutti gli ID completati

        while (!bidOrders.isEmpty() && !askOrders.isEmpty()) {
            Order bestBid = bidOrders.peek();
            Order bestAsk = askOrders.peek();

            if (bestBid.getPrice() >= bestAsk.getPrice()) {
                int tradeSize = Math.min(bestBid.getSize(), bestAsk.getSize());
                ordersToNotify.add(bestBid.getOrderId());
                ordersToNotify.add(bestAsk.getOrderId());
                executeTrade(bestBid, bestAsk, tradeSize);

                bestBid.setSize(bestBid.getSize() - tradeSize);
                bestAsk.setSize(bestAsk.getSize() - tradeSize);

                if (bestBid.getSize() == 0) bidOrders.poll();
                if (bestAsk.getSize() == 0) askOrders.poll();
            } else {
                break;
            }
        }

        updateOrdersFile();

        System.out.println(ordersToNotify);



        return ordersToNotify;
    }

    /* Esegue il trade e lo registra */
    private void executeTrade(Order bid, Order ask, int tradeSize) {
        System.out.println("Trade eseguito: " + tradeSize + " unità al prezzo " + ask.getPrice());
        saveCompletedOrder(bid.getOrderId(), ask.getOrderId(), tradeSize, ask.getPrice());
    }

    /* Salva il trade completato in CompletedOrders.json */
    private void saveCompletedOrder(int orderId, int matchedOrderId, int size, int price) {
        /*try {
            JsonArray completedOrders;
            if (Files.exists(Paths.get(COMPLETED_ORDERS_FILE))) {
                completedOrders = JsonParser.parseReader(new FileReader(COMPLETED_ORDERS_FILE)).getAsJsonArray();
            } else {
                completedOrders = new JsonArray();
            }

            JsonObject trade = new JsonObject();
            trade.addProperty("orderId", orderId);
            trade.addProperty("matchedOrderId", matchedOrderId);
            trade.addProperty("size", size);
            trade.addProperty("price", price);
            trade.addProperty("timestamp", System.currentTimeMillis());

            completedOrders.add(trade);

            try (FileWriter writer = new FileWriter(COMPLETED_ORDERS_FILE)) {
                gson.toJson(completedOrders, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    /* Rimuove gli ordini completati da Orders.json */
    private void updateOrdersFile() {
        try {
            JsonArray updatedOrders = new JsonArray();
            for (Order order : ordersMap.values()) {
                if (order.getSize() > 0) {
                    updatedOrders.add(gson.toJsonTree(order));
                }
            }
            try (FileWriter writer = new FileWriter(ORDER_FILE)) {
                gson.toJson(updatedOrders, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* Ottiene il miglior prezzo BID */
    private int getBestBidPrice() {
        return bidOrders.isEmpty() ? 0 : bidOrders.peek().getPrice();
    }

    /* Ottiene il miglior prezzo ASK */
    private int getBestAskPrice() {
        return askOrders.isEmpty() ? Integer.MAX_VALUE : askOrders.peek().getPrice();
    }

    // Comparatori
    static class BidComparator implements Comparator<Order> {
        @Override
        public int compare(Order o1, Order o2) {
            if (o1.getPrice() != o2.getPrice()) {
                return Integer.compare(o2.getPrice(), o1.getPrice());
            }
            return Long.compare(o1.getTimestamp(), o2.getTimestamp());
        }
    }

    static class AskComparator implements Comparator<Order> {
        @Override
        public int compare(Order o1, Order o2) {
            if (o1.getPrice() != o2.getPrice()) {
                return Integer.compare(o1.getPrice(), o2.getPrice());
            }
            return Long.compare(o1.getTimestamp(), o2.getTimestamp());
        }
    }


    // Classe per memorizzare informazioni sui client
    static class ClientInfo {
        InetAddress address;
        int port;

        ClientInfo(InetAddress address, int port) {
            this.address = address;
            this.port = port;
        }
    }

    // Metodo per registrare un client (quando un utente si connette)
    public void registerClient(int userId, InetAddress address, int port) {
        clients.put(userId, new ClientInfo(address, port));
        System.out.println("Client registrato: User ID " + userId + " - IP: " + address + " - Porta: " + port);
    }

    // Metodo per ascoltare le registrazioni dei client via UDP
    public void listenForRegistrations() {
        try (DatagramSocket socket = new DatagramSocket(6000)) {
            byte[] buffer = new byte[1024];
            System.out.println("Server in attesa di registrazioni dai client...");

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                if (message.startsWith("REGISTER")) {
                    String[] parts = message.split("=");
                    if (parts.length < 2) continue; // Messaggio non valido

                    int userId = Integer.parseInt(parts[1].trim());
                    InetAddress address = packet.getAddress();
                    int port = packet.getPort();

                    registerClient(userId, address, port);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    // MESSAGGIO UDP
    /**
     * prende tutti gli iD e mandare una notifica
     */
    public void MessageUDP(Set<Integer> ordersToNotify) {
        try (DatagramSocket socket = new DatagramSocket()) {
            for (Integer orderId : ordersToNotify) {
                Order order = ordersMap.get(orderId);
                if (order == null) continue;

                ClientInfo buyerInfo = clients.get(order.getBuyerId());
                ClientInfo sellerInfo = clients.get(order.getSellerId());

                String message = "Ordine completato: ID " + orderId + " | Quantità: " + order.getSize() +
                        " | Prezzo: " + order.getPrice();
                byte[] buffer = message.getBytes();

                // Invia notifica all'acquirente
                if (buyerInfo != null) {
                    DatagramPacket buyerPacket = new DatagramPacket(buffer, buffer.length, buyerInfo.address, buyerInfo.port);
                    socket.send(buyerPacket);
                }

                // Invia notifica al venditore
                if (sellerInfo != null) {
                    DatagramPacket sellerPacket = new DatagramPacket(buffer, buffer.length, sellerInfo.address, sellerInfo.port);
                    socket.send(sellerPacket);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}