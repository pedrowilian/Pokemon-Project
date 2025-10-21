package backend.network.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;

import backend.network.protocol.NetworkProtocol;
import backend.network.protocol.NetworkRequest;
import backend.network.protocol.NetworkResponse;

/**
 * Cliente de Batalhas Pokemon - Conexão com BattleServer
 * 
 * Permite jogador conectar-se a batalhas remotas
 */
public class BattleClient {
    private static final Logger LOGGER = Logger.getLogger(BattleClient.class.getName());
    private static final int TIMEOUT = 5000; // 5 segundos
    private static final int BATTLE_PORT = 5556;
    
    private final String serverHost;
    private final int serverPort;
    private final Gson gson;
    
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean connected = false;
    private Thread listenerThread;
    
    // Callback para eventos recebidos do servidor
    private Consumer<Map<String, Object>> eventCallback;
    
    public BattleClient(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;
        this.gson = NetworkProtocol.createGson();
    }
    
    public BattleClient(String host) {
        this(host, BATTLE_PORT);
    }
    
    /**
     * Conecta ao servidor de batalhas
     */
    public boolean connect() {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(serverHost, serverPort), TIMEOUT);
            
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            connected = true;
            
            // Inicia thread para escutar eventos
            startListening();
            
            LOGGER.log(Level.INFO, "✅ Conectado ao BattleServer em {0}:{1}", 
                new Object[]{serverHost, serverPort});
            return true;
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "❌ Erro ao conectar ao servidor de batalhas", e);
            return false;
        }
    }
    
    /**
     * Desconecta do servidor
     */
    public void disconnect() {
        connected = false;
        
        try {
            if (listenerThread != null) {
                listenerThread.interrupt();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            LOGGER.info("🔌 Desconectado do servidor de batalhas");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Erro ao desconectar", e);
        }
    }
    
    /**
     * Define callback para eventos do servidor
     */
    public void setEventCallback(Consumer<Map<String, Object>> callback) {
        this.eventCallback = callback;
    }
    
    /**
     * Thread que escuta eventos do servidor
     */
    private void startListening() {
        listenerThread = new Thread(() -> {
            try {
                String line;
                while (connected && (line = in.readLine()) != null) {
                    try {
                        NetworkResponse response = gson.fromJson(line, NetworkResponse.class);
                        
                        // Se tem callback, processa evento
                        if (eventCallback != null && response.getData() != null) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> data = (Map<String, Object>) response.getData();
                            
                            if (data.containsKey("event")) {
                                eventCallback.accept(data);
                            }
                        }
                        
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Erro ao processar resposta", e);
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    LOGGER.log(Level.WARNING, "Conexão perdida com servidor", e);
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
    
    /**
     * Envia requisição e aguarda resposta
     */
    private NetworkResponse sendRequest(NetworkRequest request) throws IOException {
        if (!connected) {
            throw new IOException("Não conectado ao servidor");
        }
        
        String requestJson = gson.toJson(request);
        out.println(requestJson);
        
        // Aguarda resposta (com timeout)
        String responseLine = in.readLine();
        if (responseLine == null) {
            throw new IOException("Servidor não respondeu");
        }
        
        return gson.fromJson(responseLine, NetworkResponse.class);
    }
    
    /**
     * Entra na fila de batalha
     */
    public BattleQueueResult joinQueue(String username) {
        try {
            NetworkRequest request = new NetworkRequest("BattleService", "JOIN_QUEUE", 
                Map.of("username", username));
            
            NetworkResponse response = sendRequest(request);
            
            if (!response.isSuccess()) {
                return new BattleQueueResult(false, null, null, response.getError());
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.getData();
            
            String status = (String) data.get("status");
            String battleId = (String) data.get("battleId");
            String opponent = (String) data.get("opponent");
            
            return new BattleQueueResult(true, status, battleId, opponent);
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erro ao entrar na fila", e);
            return new BattleQueueResult(false, null, null, "Erro de conexão: " + e.getMessage());
        }
    }
    
    /**
     * Ataca usando um movimento
     */
    public BattleActionResult attack(int moveIndex) {
        try {
            NetworkRequest request = new NetworkRequest("BattleService", "ATTACK", 
                Map.of("moveIndex", moveIndex));
            
            NetworkResponse response = sendRequest(request);
            
            if (!response.isSuccess()) {
                return new BattleActionResult(false, response.getError());
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.getData();
            String nextTurn = (String) data.get("nextTurn");
            
            return new BattleActionResult(true, "Ataque realizado! Próximo turno: " + nextTurn);
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erro ao atacar", e);
            return new BattleActionResult(false, "Erro de conexão: " + e.getMessage());
        }
    }
    
    /**
     * Troca de Pokemon
     */
    public BattleActionResult switchPokemon(int pokemonIndex) {
        try {
            NetworkRequest request = new NetworkRequest("BattleService", "SWITCH_POKEMON", 
                Map.of("pokemonIndex", pokemonIndex));
            
            NetworkResponse response = sendRequest(request);
            
            if (!response.isSuccess()) {
                return new BattleActionResult(false, response.getError());
            }
            
            return new BattleActionResult(true, "Pokemon trocado com sucesso!");
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erro ao trocar pokemon", e);
            return new BattleActionResult(false, "Erro de conexão: " + e.getMessage());
        }
    }
    
    /**
     * Sai da batalha
     */
    public void leaveBattle() {
        try {
            NetworkRequest request = new NetworkRequest("BattleService", "LEAVE_BATTLE", Map.of());
            sendRequest(request);
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Erro ao sair da batalha", e);
        }
    }
    
    /**
     * Resultado da fila de batalha
     */
    public static class BattleQueueResult {
        public final boolean success;
        public final String status; // "WAITING_OPPONENT", "BATTLE_STARTED", "ALREADY_IN_BATTLE"
        public final String battleId;
        public final String opponent; // Nome do oponente (se encontrado)
        
        BattleQueueResult(boolean success, String status, String battleId, String opponent) {
            this.success = success;
            this.status = status;
            this.battleId = battleId;
            this.opponent = opponent;
        }
    }
    
    /**
     * Resultado de ação de batalha
     */
    public static class BattleActionResult {
        public final boolean success;
        public final String message;
        
        BattleActionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
    
    /**
     * Teste standalone
     */
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║     POKEMON PROJECT - CLIENTE DE BATALHAS               ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();
        
        String host = args.length > 0 ? args[0] : "localhost";
        
        BattleClient client = new BattleClient(host);
        
        // Define callback para eventos
        client.setEventCallback(event -> {
            String eventType = (String) event.get("event");
            System.out.println("📡 Evento recebido: " + eventType);
            System.out.println("   Dados: " + event);
        });
        
        // Conecta
        System.out.println("🔌 Conectando ao servidor...");
        if (!client.connect()) {
            System.out.println("❌ Falha ao conectar!");
            return;
        }
        
        System.out.println("✅ Conectado!");
        
        // Entra na fila
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.print("Username: ");
            String username = reader.readLine();
            
            System.out.println("⏳ Entrando na fila de batalha...");
            BattleQueueResult result = client.joinQueue(username);
            
            if (!result.success) {
                System.out.println("❌ Erro: " + result.opponent);
                return;
            }
            
            System.out.println("✅ Status: " + result.status);
            System.out.println("   Battle ID: " + result.battleId);
            
            if ("BATTLE_STARTED".equals(result.status)) {
                System.out.println("⚔️ Oponente: " + result.opponent);
                System.out.println("🎮 Batalha iniciada!");
            } else {
                System.out.println("⏳ Aguardando oponente...");
            }
            
            // Mantém conexão
            System.out.println("\nPressione ENTER para sair...");
            reader.readLine();
            
            client.leaveBattle();
            client.disconnect();
            
        } catch (IOException e) {
            System.err.println("Erro: " + e.getMessage());
        }
    }
}
