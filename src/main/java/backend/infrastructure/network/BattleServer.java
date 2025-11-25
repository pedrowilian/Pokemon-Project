package backend.infrastructure.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import backend.application.dto.BattleStateDTO;
import backend.application.dto.PokemonDTO;
import backend.application.service.BattleService;
import backend.domain.model.BattleState;
import backend.domain.model.Pokemon;
import backend.domain.model.Team;
import backend.infrastructure.ServiceLocator;
import backend.infrastructure.network.NetworkProtocol.BattleEndMessage;
import backend.infrastructure.network.NetworkProtocol.BattleOutcomeType;
import backend.infrastructure.network.NetworkProtocol.BattleStateUpdateMessage;
import backend.infrastructure.network.NetworkProtocol.ConnectMessage;
import backend.infrastructure.network.NetworkProtocol.CreateGameMessage;
import static backend.infrastructure.network.NetworkProtocol.ERROR_INVALID_MOVE;
import static backend.infrastructure.network.NetworkProtocol.ERROR_NOT_YOUR_TURN;
import backend.infrastructure.network.NetworkProtocol.ErrorMessage;
import backend.infrastructure.network.NetworkProtocol.ForfeitMessage;
import backend.infrastructure.network.NetworkProtocol.GameCreatedMessage;
import backend.infrastructure.network.NetworkProtocol.GameErrorMessage;
import backend.infrastructure.network.NetworkProtocol.GameJoinedMessage;
import backend.infrastructure.network.NetworkProtocol.GameStartedMessage;
import backend.infrastructure.network.NetworkProtocol.JoinGameMessage;
import backend.infrastructure.network.NetworkProtocol.Message;
import backend.infrastructure.network.NetworkProtocol.PlayerMoveMessage;
import backend.infrastructure.network.NetworkProtocol.SwitchPokemonMessage;
import backend.infrastructure.network.NetworkProtocol.TurnCompleteMessage;

/**
 * Battle Server - Manages multiplayer game sessions
 * Server-authoritative architecture ensures fair play
 */
public class BattleServer {
    private static final Logger LOGGER = Logger.getLogger(BattleServer.class.getName());
    private static final int DEFAULT_PORT = 8888;
    
    private final int port;
    private ServerSocket serverSocket;
    private final Map<String, GameSession> gameSessions;
    private final Map<String, ClientHandler> connectedClients;
    private ClientHandler waitingPlayer; // Player waiting for opponent
    private boolean running;
    
    public BattleServer(int port) {
        this.port = port;
        this.gameSessions = new ConcurrentHashMap<>();
        this.connectedClients = new ConcurrentHashMap<>();
        this.running = false;
    }
    
    public BattleServer() {
        this(DEFAULT_PORT);
    }
    
    /**
     * Start the server
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        LOGGER.info("Battle Server started on port " + port);
        
        // Accept client connections
        new Thread(() -> {
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    LOGGER.info("New client connected: " + clientSocket.getInetAddress());
                    
                    ClientHandler handler = new ClientHandler(clientSocket);
                    new Thread(handler).start();
                    
                } catch (IOException e) {
                    if (running) {
                        LOGGER.log(Level.SEVERE, "Error accepting client", e);
                    }
                }
            }
        }).start();
    }
    
    /**
     * Stop the server
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            // Close all client connections
            connectedClients.values().forEach(ClientHandler::disconnect);
            connectedClients.clear();
            gameSessions.clear();
            LOGGER.info("Battle Server stopped");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error stopping server", e);
        }
    }
    
    /**
     * Automatic matchmaking - add player to queue
     * Thread-safe: synchronized to prevent race conditions
     * 
     * Flow:
     * - 1st player: Added to waitingPlayer queue
     * - 2nd player: Matched with waitingPlayer, game starts immediately
     * - 3rd+ player: Replaces waitingPlayer (previous waiter is notified)
     * 
     * This ensures only 2 players are in a match at a time, and one player
     * is always waiting for the next available opponent.
     */
    private synchronized void matchmakePlayer(String playerName, List<PokemonDTO> team, ClientHandler handler) {
        if (waitingPlayer == null) {
            // First player or new player after match started - put in waiting queue
            waitingPlayer = handler;
            waitingPlayer.setTeam(team);
            waitingPlayer.sendMessage(new GameCreatedMessage("WAITING", true));
            LOGGER.info("Player " + playerName + " is waiting for opponent");
        } else {
            // Second player - start game immediately
            if (waitingPlayer == handler) {
                // Same player trying to join twice - should not happen
                handler.sendMessage(new GameErrorMessage(
                    "You are already in the matchmaking queue"));
                return;
            }
            
            String gameId = generateGameId();
            GameSession session = new GameSession(
                gameId, 
                waitingPlayer.getUsername(), 
                waitingPlayer.getTeam(), 
                waitingPlayer,
                playerName,
                team,
                handler
            );
            gameSessions.put(gameId, session);
            
            // Notify both players
            waitingPlayer.sendMessage(new GameJoinedMessage(gameId, true, playerName));
            handler.sendMessage(new GameJoinedMessage(gameId, false, waitingPlayer.getUsername()));
            
            LOGGER.info("Match created: " + gameId + " - " + waitingPlayer.getUsername() + " vs " + playerName);
            
            // Start battle
            session.startBattle();
            
            // Clear waiting player
            waitingPlayer = null;
        }
    }
    
    /**
     * Generate a unique game ID
     */
    private String generateGameId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Handle client disconnection
     */
    private void handleClientDisconnect(ClientHandler client) {
        connectedClients.remove(client.getClientId());
        
        // Check if this is the waiting player
        if (waitingPlayer == client) {
            waitingPlayer = null;
            LOGGER.info("Waiting player disconnected");
            return;
        }
        
        // Find and end any games this client was in
        gameSessions.values().stream()
            .filter(session -> session.hasClient(client))
            .forEach(session -> {
                session.handlePlayerDisconnect(client);
                gameSessions.remove(session.getGameId());
            });
    }
    
    /**
     * Game Session - Represents a single multiplayer battle
     */
    private class GameSession {
        private final String gameId;
        private final String player1Name;
        private final String player2Name;
        private final List<PokemonDTO> player1Team;
        private final List<PokemonDTO> player2Team;
        private final ClientHandler player1Handler;
        private final ClientHandler player2Handler;
        private final BattleService battleService;
        private BattleState battleState; // Domain model
        private BattleStateDTO currentState;
        private boolean isPlayer1Turn;
        private boolean battleStarted;
        
        // Constructor for immediate matchmaking
        public GameSession(String gameId, String player1Name, List<PokemonDTO> team1, ClientHandler handler1,
                          String player2Name, List<PokemonDTO> team2, ClientHandler handler2) {
            this.gameId = gameId;
            this.player1Name = player1Name;
            this.player1Team = team1;
            this.player1Handler = handler1;
            this.player2Name = player2Name;
            this.player2Team = team2;
            this.player2Handler = handler2;
            this.battleService = ServiceLocator.getInstance().getBattleService();
            this.battleStarted = false;
            this.isPlayer1Turn = true;
        }
        
        public boolean hasClient(ClientHandler client) {
            return client == player1Handler || client == player2Handler;
        }
        
        public String getGameId() {
            return gameId;
        }
        
        /**
         * Start the battle
         */
        public void startBattle() {
            try {
                // Convert DTOs to Pokemon objects
                List<Pokemon> team1 = player1Team.stream()
                    .map(this::convertDTOToPokemon)
                    .collect(Collectors.toList());
                List<Pokemon> team2 = player2Team.stream()
                    .map(this::convertDTOToPokemon)
                    .collect(Collectors.toList());
                
                // Create Teams
                Team playerTeamObj = battleService.createTeam(team1, player1Name);
                Team enemyTeamObj = battleService.createTeam(team2, player2Name);
                
                // Initialize battle
                battleState = battleService.startBattle(playerTeamObj, enemyTeamObj);
                currentState = battleService.getBattleStateDTO(battleState);
                battleStarted = true;
                
                // Notify both players
                player1Handler.sendMessage(new GameStartedMessage(currentState));
                player2Handler.sendMessage(new GameStartedMessage(currentState));
                
                LOGGER.info("Battle started: " + gameId);
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error starting battle", e);
                player1Handler.sendMessage(new GameErrorMessage("Failed to start battle"));
                player2Handler.sendMessage(new GameErrorMessage("Failed to start battle"));
            }
        }
        
        /**
         * Process a player move
         */
        public synchronized void processMove(ClientHandler client, int moveIndex) {
            if (!battleStarted) {
                client.sendMessage(new ErrorMessage(ERROR_INVALID_MOVE, "Battle not started"));
                return;
            }
            
            // Validate it's the correct player's turn
            boolean isPlayer1 = (client == player1Handler);
            if (isPlayer1 != isPlayer1Turn) {
                client.sendMessage(new ErrorMessage(ERROR_NOT_YOUR_TURN, "Not your turn"));
                return;
            }
            
            try {
                // Execute the move on server
                String message = battleService.executePlayerMove(battleState, moveIndex);
                
                // Get updated state
                currentState = battleService.getBattleStateDTO(battleState);
                
                // Send update to both players
                BattleStateUpdateMessage update = new BattleStateUpdateMessage(currentState, message);
                player1Handler.sendMessage(update);
                player2Handler.sendMessage(update);
                
                // Check if battle ended
                if (currentState.isBattleEnded()) {
                    handleBattleEnd();
                } else {
                    // Switch turns
                    isPlayer1Turn = !isPlayer1Turn;
                    player1Handler.sendMessage(new TurnCompleteMessage());
                    player2Handler.sendMessage(new TurnCompleteMessage());
                }
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error processing move", e);
                client.sendMessage(new ErrorMessage(ERROR_INVALID_MOVE, e.getMessage()));
            }
        }
        
        /**
         * Process a Pokemon switch
         */
        public synchronized void processSwitchPokemon(ClientHandler client, int pokemonIndex) {
            if (!battleStarted) {
                client.sendMessage(new ErrorMessage(ERROR_INVALID_MOVE, "Battle not started"));
                return;
            }
            
            boolean isPlayer1 = (client == player1Handler);
            if (isPlayer1 != isPlayer1Turn) {
                client.sendMessage(new ErrorMessage(ERROR_NOT_YOUR_TURN, "Not your turn"));
                return;
            }
            
            try {
                // Execute the switch on server
                String message = battleService.switchPokemon(battleState, pokemonIndex);
                
                // Get updated state
                currentState = battleService.getBattleStateDTO(battleState);
                
                // Send update to both players
                BattleStateUpdateMessage update = new BattleStateUpdateMessage(currentState, message);
                player1Handler.sendMessage(update);
                player2Handler.sendMessage(update);
                
                // Switch turns
                isPlayer1Turn = !isPlayer1Turn;
                player1Handler.sendMessage(new TurnCompleteMessage());
                player2Handler.sendMessage(new TurnCompleteMessage());
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error switching Pokemon", e);
                client.sendMessage(new ErrorMessage(ERROR_INVALID_MOVE, e.getMessage()));
            }
        }
        
        /**
         * Handle battle end
         */
        private void handleBattleEnd() {
            String winnerName = currentState.getWinner();
            boolean player1Won = winnerName.equals(player1Name);
            String loserName = player1Won ? player2Name : player1Name;

            BattleEndMessage endMessage = new BattleEndMessage(
                player1Won,
                winnerName,
                loserName,
                BattleOutcomeType.NORMAL
            );
            player1Handler.sendMessage(endMessage);
            player2Handler.sendMessage(endMessage);
            
            LOGGER.info("Battle ended: " + gameId + " - Winner: " + winnerName);
            
            // Remove session
            gameSessions.remove(gameId);
        }

        /**
         * Handle an explicit forfeit from a player
         */
        public synchronized void processForfeit(ClientHandler forfeitingClient, String reason) {
            if (!battleStarted) {
                handlePlayerDisconnect(forfeitingClient);
                return;
            }

            boolean forfeitingIsPlayer1 = (forfeitingClient == player1Handler);
            String forfeitingName = forfeitingIsPlayer1 ? player1Name : player2Name;
            String winnerName = forfeitingIsPlayer1 ? player2Name : player1Name;
            boolean didPlayer1Win = !forfeitingIsPlayer1;

            if (currentState != null) {
                currentState.setBattleEnded(true);
                currentState.setWinner(winnerName);
            }

            BattleEndMessage endMessage = new BattleEndMessage(
                didPlayer1Win,
                winnerName,
                forfeitingName,
                BattleOutcomeType.FORFEIT
            );
            player1Handler.sendMessage(endMessage);
            player2Handler.sendMessage(endMessage);

            String reasonSuffix = (reason != null && !reason.isBlank()) ? " (" + reason + ")" : "";
            LOGGER.info(() -> "Player forfeited: " + forfeitingName + reasonSuffix + " - winner: " + winnerName);

            battleStarted = false;
            gameSessions.remove(gameId);
        }
        
        /**
         * Handle player disconnect
         */
        public void handlePlayerDisconnect(ClientHandler disconnectedClient) {
            if (!battleStarted) {
                return;
            }
            
            ClientHandler remainingClient = (disconnectedClient == player1Handler) ? player2Handler : player1Handler;
            String winnerName = (disconnectedClient == player1Handler) ? player2Name : player1Name;
            
            if (remainingClient != null) {
                boolean player1Won = remainingClient == player1Handler;
                String loserName = disconnectedClient == player1Handler ? player1Name : player2Name;
                remainingClient.sendMessage(new BattleEndMessage(
                    player1Won,
                    winnerName,
                    loserName,
                    BattleOutcomeType.DISCONNECT
                ));
            }
            
            LOGGER.info("Player disconnected from game: " + gameId);
            
            // FIXED: Remove session from map to prevent memory leak
            gameSessions.remove(gameId);
            battleStarted = false;
        }
        
        /**
         * Convert DTO to Pokemon - creates Pokemon directly from DTO data
         * No database access needed - client already sent all data
         */
        private Pokemon convertDTOToPokemon(PokemonDTO dto) {
            return new Pokemon(
                dto.getId(),
                dto.getName(),
                dto.getForm(),
                dto.getType1(),
                dto.getType2(),
                dto.getTotal(),
                dto.getHp(),
                dto.getAttack(),
                dto.getDefense(),
                dto.getSpAtk(),
                dto.getSpDef(),
                dto.getSpeed(),
                dto.getGeneration()
            );
        }
    }
    
    /**
     * Client Handler - Handles individual client connections
     */
    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final String clientId;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private String username;
        private List<PokemonDTO> team;
        private boolean connected;
        
        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.clientId = UUID.randomUUID().toString();
            this.connected = true;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setTeam(List<PokemonDTO> team) {
            this.team = team;
        }
        
        public List<PokemonDTO> getTeam() {
            return team;
        }
        
        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                
                connectedClients.put(clientId, this);
                
                // Handle messages
                while (connected && !socket.isClosed()) {
                    try {
                        Message message = (Message) in.readObject();
                        handleMessage(message);
                    } catch (ClassNotFoundException e) {
                        LOGGER.log(Level.SEVERE, "Invalid message received", e);
                    }
                }
                
            } catch (IOException e) {
                if (connected) {
                    LOGGER.log(Level.WARNING, "Client connection error", e);
                }
            } finally {
                disconnect();
            }
        }
        
        /**
         * Handle incoming message
         */
        private void handleMessage(Message message) {
            switch (message.getType()) {
                case CONNECT:
                    handleConnect((ConnectMessage) message);
                    break;
                case CREATE_GAME:
                    handleCreateGame((CreateGameMessage) message);
                    break;
                case JOIN_GAME:
                    handleJoinGame((JoinGameMessage) message);
                    break;
                case PLAYER_MOVE:
                    handlePlayerMove((PlayerMoveMessage) message);
                    break;
                case SWITCH_POKEMON:
                    handleSwitchPokemon((SwitchPokemonMessage) message);
                    break;
                case FORFEIT:
                    handleForfeit((ForfeitMessage) message);
                    break;
                case DISCONNECT:
                    disconnect();
                    break;
                case HEARTBEAT:
                    // Just acknowledge
                    break;
                default:
                    LOGGER.warning("Unhandled message type: " + message.getType());
            }
        }
        
        private void handleConnect(ConnectMessage message) {
            this.username = message.getUsername();
            LOGGER.info("Client connected: " + username);
        }
        
        private void handleCreateGame(CreateGameMessage message) {
            // Automatic matchmaking - no manual game creation
            matchmakePlayer(message.getPlayerName(), message.getTeam(), this);
        }
        
        private void handleJoinGame(JoinGameMessage message) {
            // In automatic matchmaking, JOIN_GAME also triggers matchmaking
            // This handles the "connect and wait" functionality
            matchmakePlayer(message.getPlayerName(), message.getTeam(), this);
        }
        
        private void handlePlayerMove(PlayerMoveMessage message) {
            // Find the game session this client is in
            gameSessions.values().stream()
                .filter(session -> session.hasClient(this))
                .findFirst()
                .ifPresent(session -> session.processMove(this, message.getMoveIndex()));
        }
        
        private void handleSwitchPokemon(SwitchPokemonMessage message) {
            // Find the game session this client is in
            gameSessions.values().stream()
                .filter(session -> session.hasClient(this))
                .findFirst()
                .ifPresent(session -> session.processSwitchPokemon(this, message.getPokemonIndex()));
        }

        private void handleForfeit(ForfeitMessage message) {
            gameSessions.values().stream()
                .filter(session -> session.hasClient(this))
                .findFirst()
                .ifPresent(session -> session.processForfeit(this, message.getReason()));
        }
        
        /**
         * Send message to client
         */
        public void sendMessage(Message message) {
            try {
                if (out != null && connected) {
                    out.writeObject(message);
                    out.flush();
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error sending message to client", e);
                disconnect();
            }
        }
        
        /**
         * Disconnect client
         */
        public void disconnect() {
            connected = false;
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                handleClientDisconnect(this);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error disconnecting client", e);
            }
        }
        
        public String getClientId() {
            return clientId;
        }
    }
    
    /**
     * Main method to start the server
     */
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default: " + DEFAULT_PORT);
            }
        }
        
        BattleServer server = new BattleServer(port);
        try {
            server.start();
            System.out.println("🎮 Pokemon Battle Server running on port " + port);
            System.out.println("Press Ctrl+C to stop");
            
            // Keep server running
            Thread.currentThread().join();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Server error", e);
            server.stop();
        }
    }
}
