package backend.infrastructure.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import backend.application.dto.BattleStateDTO;
import backend.application.dto.PokemonDTO;
import backend.infrastructure.network.NetworkProtocol.BattleEndMessage;
import backend.infrastructure.network.NetworkProtocol.BattleStateUpdateMessage;
import backend.infrastructure.network.NetworkProtocol.ConnectMessage;
import backend.infrastructure.network.NetworkProtocol.CreateGameMessage;
import backend.infrastructure.network.NetworkProtocol.DisconnectMessage;
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
 * Battle Client - Handles client-side network communication
 * Connects to BattleServer and manages game state synchronization
 */
public class BattleClient {
    private static final Logger LOGGER = Logger.getLogger(BattleClient.class.getName());
    
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean connected;
    private Thread receiveThread;
    private final BlockingQueue<Message> messageQueue;
    private BattleClientListener listener;
    
    private String gameId;
    private boolean isPlayerOne;
    
    public BattleClient() {
        this.messageQueue = new LinkedBlockingQueue<>();
        this.connected = false;
    }
    
    /**
     * Connect to the server
     */
    public boolean connect(String host, int port, String username) {
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;
            
            // Send connection message
            sendMessage(new ConnectMessage(username, NetworkProtocol.PROTOCOL_VERSION));
            
            // Start receiving messages
            startReceiving();
            
            LOGGER.info("Connected to server: " + host + ":" + port);
            return true;
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to server", e);
            return false;
        }
    }
    
    /**
     * Disconnect from server
     */
    public void disconnect() {
        if (!connected) {
            return;
        }
        
        try {
            sendMessage(new DisconnectMessage("Client disconnect"));
            connected = false;
            
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            
            if (receiveThread != null) {
                receiveThread.interrupt();
            }
            
            LOGGER.info("Disconnected from server");
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error disconnecting", e);
        }
    }
    
    /**
     * Create a new game
     */
    public void createGame(String playerName, List<PokemonDTO> team) {
        if (!connected) {
            LOGGER.warning("Not connected to server");
            return;
        }
        
        sendMessage(new CreateGameMessage(playerName, team));
    }
    
    /**
     * Join an existing game
     */
    public void joinGame(String gameId, String playerName, List<PokemonDTO> team) {
        if (!connected) {
            LOGGER.warning("Not connected to server");
            return;
        }
        
        sendMessage(new JoinGameMessage(gameId, playerName, team));
    }
    
    /**
     * Send a move to the server
     */
    public void sendMove(int moveIndex) {
        if (!connected) {
            LOGGER.warning("Not connected to server");
            return;
        }
        
        sendMessage(new PlayerMoveMessage(moveIndex));
    }
    
    /**
     * Send a Pokemon switch to the server
     */
    public void sendSwitchPokemon(int pokemonIndex) {
        if (!connected) {
            LOGGER.warning("Not connected to server");
            return;
        }
        
        sendMessage(new SwitchPokemonMessage(pokemonIndex));
    }

    /**
     * Send a forfeit request to the server
     */
    public void sendForfeit(String reason) {
        if (!connected) {
            LOGGER.warning("Not connected to server");
            return;
        }

        sendMessage(new ForfeitMessage(reason));
    }
    
    /**
     * Send a message to the server
     */
    private void sendMessage(Message message) {
        try {
            if (out != null && connected) {
                out.writeObject(message);
                out.flush();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error sending message", e);
            handleConnectionLost();
        }
    }
    
    /**
     * Start receiving messages from server
     */
    private void startReceiving() {
        receiveThread = new Thread(() -> {
            while (connected && !Thread.interrupted()) {
                try {
                    Message message = (Message) in.readObject();
                    handleMessage(message);
                    
                } catch (IOException e) {
                    if (connected) {
                        LOGGER.log(Level.SEVERE, "Connection lost", e);
                        handleConnectionLost();
                    }
                    break;
                } catch (ClassNotFoundException e) {
                    LOGGER.log(Level.SEVERE, "Invalid message received", e);
                }
            }
        });
        receiveThread.setDaemon(true);
        receiveThread.start();
    }
    
    /**
     * Handle incoming message
     */
    private void handleMessage(Message message) {
        if (listener == null) {
            return;
        }
        
        switch (message.getType()) {
            case GAME_CREATED:
                handleGameCreated((GameCreatedMessage) message);
                break;
            case GAME_JOINED:
                handleGameJoined((GameJoinedMessage) message);
                break;
            case GAME_STARTED:
                handleGameStarted((GameStartedMessage) message);
                break;
            case BATTLE_STATE_UPDATE:
                handleBattleStateUpdate((BattleStateUpdateMessage) message);
                break;
            case TURN_COMPLETE:
                handleTurnComplete((TurnCompleteMessage) message);
                break;
            case BATTLE_END:
                handleBattleEnd((BattleEndMessage) message);
                break;
            case ERROR:
                handleError((ErrorMessage) message);
                break;
            case GAME_ERROR:
                handleGameError((GameErrorMessage) message);
                break;
            default:
                LOGGER.warning("Unhandled message type: " + message.getType());
        }
    }
    
    private void handleGameCreated(GameCreatedMessage message) {
        this.gameId = message.getGameId();
        this.isPlayerOne = message.isPlayerOne();
        
        if (listener != null) {
            listener.onGameCreated(message.getGameId());
        }
    }
    
    private void handleGameJoined(GameJoinedMessage message) {
        this.gameId = message.getGameId();
        this.isPlayerOne = message.isPlayerOne();
        
        if (listener != null) {
            listener.onGameJoined(message.getGameId(), message.getOpponentName());
        }
    }
    
    private void handleGameStarted(GameStartedMessage message) {
        if (listener != null) {
            listener.onBattleStarted(message.getInitialState());
        }
    }
    
    private void handleBattleStateUpdate(BattleStateUpdateMessage message) {
        if (listener != null) {
            listener.onBattleStateUpdate(message.getState(), message.getActionMessage());
        }
    }
    
    private void handleTurnComplete(TurnCompleteMessage message) {
        if (listener != null) {
            listener.onTurnComplete();
        }
    }
    
    private void handleBattleEnd(BattleEndMessage message) {
        if (listener != null) {
            boolean didIWin = (isPlayerOne && message.isPlayerOneWon()) || 
                            (!isPlayerOne && !message.isPlayerOneWon());
            listener.onBattleEnd(
                didIWin,
                message.getWinnerName(),
                message.getLoserName(),
                message.getOutcomeType()
            );
        }
    }
    
    private void handleError(ErrorMessage message) {
        LOGGER.warning("Error from server: " + message.getErrorMessage());
        
        if (listener != null) {
            listener.onError(message.getErrorCode(), message.getErrorMessage());
        }
    }
    
    private void handleGameError(GameErrorMessage message) {
        LOGGER.warning("Game error: " + message.getError());
        
        if (listener != null) {
            listener.onGameError(message.getError());
        }
    }
    
    /**
     * Handle connection lost
     */
    private void handleConnectionLost() {
        connected = false;
        
        if (listener != null) {
            listener.onConnectionLost();
        }
    }
    
    /**
     * Set the listener for client events
     */
    public void setListener(BattleClientListener listener) {
        this.listener = listener;
    }
    
    /**
     * Check if connected
     */
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
    
    /**
     * Get current game ID
     */
    public String getGameId() {
        return gameId;
    }
    
    /**
     * Check if this client is player one
     */
    public boolean isPlayerOne() {
        return isPlayerOne;
    }
    
    /**
     * Listener interface for client events
     */
    public interface BattleClientListener {
        /**
         * Called when a game is successfully created
         */
        void onGameCreated(String gameId);
        
        /**
         * Called when successfully joined a game
         */
        void onGameJoined(String gameId, String opponentName);
        
        /**
         * Called when the battle starts
         */
        void onBattleStarted(BattleStateDTO initialState);
        
        /**
         * Called when battle state is updated
         */
        void onBattleStateUpdate(BattleStateDTO state, String actionMessage);
        
        /**
         * Called when a turn is complete
         */
        void onTurnComplete();
        
        /**
         * Called when the battle ends
         */
        void onBattleEnd(boolean didIWin, String winnerName, String loserName, NetworkProtocol.BattleOutcomeType outcomeType);
        
        /**
         * Called when an error occurs
         */
        void onError(String errorCode, String errorMessage);
        
        /**
         * Called when a game error occurs
         */
        void onGameError(String error);
        
        /**
         * Called when connection is lost
         */
        void onConnectionLost();
    }
}
