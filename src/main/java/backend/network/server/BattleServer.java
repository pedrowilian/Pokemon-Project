package backend.network.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;

import backend.application.dto.BattleStateDTO;
import backend.application.service.BattleService;
import backend.infrastructure.ServiceLocator;
import backend.infrastructure.security.RateLimiter;
import backend.network.protocol.NetworkProtocol;
import backend.network.protocol.NetworkRequest;
import backend.network.protocol.NetworkResponse;

/**
 * Servidor de Batalhas Pokemon - Multiplayer Remoto
 * 
 * Gerencia batalhas entre 2 jogadores em máquinas diferentes
 * Protocolo: JSON sobre TCP Socket
 */
public class BattleServer {
    private static final Logger LOGGER = Logger.getLogger(BattleServer.class.getName());
    private static final int PORT = 5556; // Porta diferente do AuthServer
    private static final int MAX_CONNECTIONS = 20; // 10 batalhas simultâneas (2 jogadores por batalha)
    
    private final ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private final Gson gson;
    private final BattleService battleService;
    
    // Gerenciamento de batalhas ativas
    private final Map<String, BattleRoom> battleRooms; // battleId -> BattleRoom
    private final Map<String, String> playerToBattle; // username -> battleId
    private final Object lock = new Object();
    
    public BattleServer() throws IOException {
        this.serverSocket = new ServerSocket(PORT);
        this.threadPool = Executors.newFixedThreadPool(MAX_CONNECTIONS);
        this.gson = NetworkProtocol.createGson();
        this.battleService = ServiceLocator.getInstance().getBattleService();
        this.battleRooms = new ConcurrentHashMap<>();
        this.playerToBattle = new ConcurrentHashMap<>();
        
        LOGGER.log(Level.INFO, "🎮 BattleServer iniciado na porta {0}", PORT);
    }
    
    public void start() {
        LOGGER.info("⚔️ Servidor de batalhas aguardando conexões...");
        
        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                String clientIP = clientSocket.getInetAddress().getHostAddress();
                
                // Rate limiting - proteção contra DDoS
                if (!RateLimiter.allowRequest(clientIP)) {
                    LOGGER.log(Level.WARNING, "� Conexão bloqueada (rate limit): {0}", clientIP);
                    clientSocket.close();
                    continue;
                }
                
                LOGGER.log(Level.INFO, "🔌 Cliente conectado: {0}", clientIP);
                
                threadPool.execute(new BattleClientHandler(clientSocket));
                
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    LOGGER.log(Level.SEVERE, "❌ Erro ao aceitar conexão", e);
                }
            }
        }
    }
    
    /**
     * Sala de batalha - contém 2 jogadores e o estado da batalha
     */
    private class BattleRoom {
        final String battleId;
        final String player1;
        String player2; // Não é final pois será atribuído depois
        BattleStateDTO currentState;
        PrintWriter player1Writer;
        PrintWriter player2Writer;
        String currentTurn; // username do jogador atual
        boolean isActive = true;
        
        BattleRoom(String battleId, String player1, String player2) {
            this.battleId = battleId;
            this.player1 = player1;
            this.player2 = player2;
            this.currentTurn = player1; // Player 1 começa
        }
        
        boolean isPlayerTurn(String username) {
            return currentTurn.equals(username);
        }
        
        void switchTurn() {
            currentTurn = currentTurn.equals(player1) ? player2 : player1;
        }
        
        void broadcast(NetworkResponse response) {
            String json = gson.toJson(response);
            if (player1Writer != null) {
                player1Writer.println(json);
            }
            if (player2Writer != null) {
                player2Writer.println(json);
            }
        }
    }
    
    /**
     * Handler para cada cliente conectado
     */
    private class BattleClientHandler implements Runnable {
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String username;
        private String currentBattleId;
        
        BattleClientHandler(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                
                String line;
                while ((line = in.readLine()) != null) {
                    try {
                        NetworkRequest request = gson.fromJson(line, NetworkRequest.class);
                        NetworkResponse response = processRequest(request);
                        
                        if (response != null) {
                            out.println(gson.toJson(response));
                        }
                        
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Erro ao processar requisição", e);
                        NetworkResponse errorResponse = NetworkResponse.error("", "Erro ao processar: " + e.getMessage());
                        out.println(gson.toJson(errorResponse));
                    }
                }
                
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Cliente desconectado: {0}", username);
            } finally {
                cleanup();
            }
        }
        
        private NetworkResponse processRequest(NetworkRequest request) {
            String method = request.getMethod();
            
            switch (method) {
                case "JOIN_QUEUE":
                    return handleJoinQueue(request);
                    
                case "CREATE_BATTLE":
                    return handleCreateBattle(request);
                    
                case "ATTACK":
                    return handleAttack(request);
                    
                case "SWITCH_POKEMON":
                    return handleSwitchPokemon(request);
                    
                case "GET_STATE":
                    return handleGetState(request);
                    
                case "LEAVE_BATTLE":
                    return handleLeaveBattle(request);
                    
                default:
                    return NetworkResponse.error(request.getRequestId(), "Método desconhecido: " + method);
            }
        }
        
        private NetworkResponse handleJoinQueue(NetworkRequest request) {
            username = (String) request.getParams().get("username");
            
            synchronized (lock) {
                // Verifica se já está em uma batalha
                if (playerToBattle.containsKey(username)) {
                    String battleId = playerToBattle.get(username);
                    BattleRoom room = battleRooms.get(battleId);
                    if (room != null && room.isActive) {
                        return NetworkResponse.success(request.getRequestId(), 
                            Map.of("status", "ALREADY_IN_BATTLE", "battleId", battleId));
                    }
                }
                
                // Procura uma sala esperando jogador
                for (BattleRoom room : battleRooms.values()) {
                    if (room.player2 == null && !room.player1.equals(username)) {
                        // Adiciona como player 2
                        room.player2 = username;
                        room.player2Writer = out;
                        playerToBattle.put(username, room.battleId);
                        currentBattleId = room.battleId;
                        
                        LOGGER.log(Level.INFO, "⚔️ Batalha iniciada: {0} vs {1}", 
                            new Object[]{room.player1, room.player2});
                        
                        return NetworkResponse.success(request.getRequestId(), 
                            Map.of("status", "BATTLE_STARTED", "battleId", room.battleId,
                                   "opponent", room.player1));
                    }
                }
                
                // Cria nova sala esperando oponente
                String battleId = "battle-" + UUID.randomUUID().toString().substring(0, 8);
                BattleRoom room = new BattleRoom(battleId, username, null);
                room.player1Writer = out;
                battleRooms.put(battleId, room);
                playerToBattle.put(username, battleId);
                currentBattleId = battleId;
                
                LOGGER.log(Level.INFO, "⏳ {0} aguardando oponente...", username);
                
                return NetworkResponse.success(request.getRequestId(), 
                    Map.of("status", "WAITING_OPPONENT", "battleId", battleId));
            }
        }
        
        private NetworkResponse handleCreateBattle(NetworkRequest request) {
            String battleId = playerToBattle.get(username);
            if (battleId == null) {
                return NetworkResponse.error(request.getRequestId(), "Jogador não está em fila");
            }
            
            BattleRoom room = battleRooms.get(battleId);
            if (room == null) {
                return NetworkResponse.error(request.getRequestId(), "Sala de batalha não encontrada");
            }
            
            if (room.player2 == null) {
                return NetworkResponse.error(request.getRequestId(), "Aguardando oponente");
            }
            
            // Recebe times dos jogadores
            @SuppressWarnings("unchecked")
            Map<String, Object> player1TeamData = (Map<String, Object>) request.getParams().get("player1Team");
            @SuppressWarnings("unchecked")
            Map<String, Object> player2TeamData = (Map<String, Object>) request.getParams().get("player2Team");
            
            // Aqui você converteria os dados em TeamDTO
            // Por simplicidade, vamos assumir que a batalha já foi criada
            
            room.currentState = new BattleStateDTO();
            // TODO: Inicializar estado da batalha com os times
            
            return NetworkResponse.success(request.getRequestId(), 
                Map.of("status", "BATTLE_READY", "currentTurn", room.currentTurn));
        }
        
        private NetworkResponse handleAttack(NetworkRequest request) {
            String battleId = playerToBattle.get(username);
            if (battleId == null) {
                return NetworkResponse.error(request.getRequestId(), "Não está em batalha");
            }
            
            BattleRoom room = battleRooms.get(battleId);
            if (room == null) {
                return NetworkResponse.error(request.getRequestId(), "Sala não encontrada");
            }
            
            if (!room.isPlayerTurn(username)) {
                return NetworkResponse.error(request.getRequestId(), "Não é seu turno");
            }
            
            int moveIndex = ((Double) request.getParams().get("moveIndex")).intValue();
            
            // Processa ataque (delegado ao BattleService)
            // TODO: Integrar com BattleService real
            
            // Muda turno
            room.switchTurn();
            
            // Broadcast resultado para ambos jogadores
            NetworkResponse broadcast = NetworkResponse.success("", 
                Map.of("event", "ATTACK", "attacker", username, "moveIndex", moveIndex,
                       "nextTurn", room.currentTurn));
            room.broadcast(broadcast);
            
            return NetworkResponse.success(request.getRequestId(), 
                Map.of("status", "ATTACK_SUCCESS", "nextTurn", room.currentTurn));
        }
        
        private NetworkResponse handleSwitchPokemon(NetworkRequest request) {
            String battleId = playerToBattle.get(username);
            if (battleId == null) {
                return NetworkResponse.error(request.getRequestId(), "Não está em batalha");
            }
            
            BattleRoom room = battleRooms.get(battleId);
            if (room == null) {
                return NetworkResponse.error(request.getRequestId(), "Sala não encontrada");
            }
            
            if (!room.isPlayerTurn(username)) {
                return NetworkResponse.error(request.getRequestId(), "Não é seu turno");
            }
            
            int pokemonIndex = ((Double) request.getParams().get("pokemonIndex")).intValue();
            
            // TODO: Processar troca de Pokemon
            
            room.switchTurn();
            
            NetworkResponse broadcast = NetworkResponse.success("", 
                Map.of("event", "SWITCH", "player", username, "pokemonIndex", pokemonIndex,
                       "nextTurn", room.currentTurn));
            room.broadcast(broadcast);
            
            return NetworkResponse.success(request.getRequestId(), 
                Map.of("status", "SWITCH_SUCCESS", "nextTurn", room.currentTurn));
        }
        
        private NetworkResponse handleGetState(NetworkRequest request) {
            String battleId = playerToBattle.get(username);
            if (battleId == null) {
                return NetworkResponse.error(request.getRequestId(), "Não está em batalha");
            }
            
            BattleRoom room = battleRooms.get(battleId);
            if (room == null) {
                return NetworkResponse.error(request.getRequestId(), "Sala não encontrada");
            }
            
            return NetworkResponse.success(request.getRequestId(), 
                Map.of("state", room.currentState, "currentTurn", room.currentTurn));
        }
        
        private NetworkResponse handleLeaveBattle(NetworkRequest request) {
            String battleId = playerToBattle.get(username);
            if (battleId == null) {
                return NetworkResponse.success(request.getRequestId(), 
                    Map.of("status", "NOT_IN_BATTLE"));
            }
            
            BattleRoom room = battleRooms.get(battleId);
            if (room != null) {
                room.isActive = false;
                
                // Notifica oponente
                NetworkResponse broadcast = NetworkResponse.success("", 
                    Map.of("event", "OPPONENT_LEFT", "player", username));
                room.broadcast(broadcast);
                
                battleRooms.remove(battleId);
            }
            
            playerToBattle.remove(username);
            currentBattleId = null;
            
            return NetworkResponse.success(request.getRequestId(), 
                Map.of("status", "LEFT_BATTLE"));
        }
        
        private void cleanup() {
            try {
                if (currentBattleId != null) {
                    handleLeaveBattle(new NetworkRequest("", "LEAVE_BATTLE", Map.of()));
                }
                
                socket.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Erro ao fechar socket", e);
            }
        }
    }
    
    public void stop() {
        try {
            threadPool.shutdown();
            serverSocket.close();
            LOGGER.info("🛑 BattleServer encerrado");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erro ao encerrar servidor", e);
        }
    }
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║     POKEMON PROJECT - SERVIDOR DE BATALHAS              ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();
        
        try {
            BattleServer server = new BattleServer();
            
            // Shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n🛑 Encerrando servidor...");
                server.stop();
            }));
            
            server.start();
            
        } catch (IOException e) {
            System.err.println("❌ Erro ao iniciar servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
