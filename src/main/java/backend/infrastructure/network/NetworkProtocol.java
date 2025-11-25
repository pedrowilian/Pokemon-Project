package backend.infrastructure.network;

import java.io.Serializable;
import java.util.List;

import backend.application.dto.BattleStateDTO;
import backend.application.dto.PokemonDTO;

/**
 * Network Protocol - Defines all message types for client-server communication
 * This class contains all DTOs and message types used in multiplayer battles
 */
public class NetworkProtocol {

    /**
     * Reasons a battle can end. Used to provide better UX and localization.
     */
    public enum BattleOutcomeType {
        NORMAL,
        FORFEIT,
        DISCONNECT
    }
    
    // Message Types
    public enum MessageType {
        // Connection messages
        CONNECT,
        DISCONNECT,
        HEARTBEAT,
        
        // Game setup messages
        CREATE_GAME,
        JOIN_GAME,
        GAME_CREATED,
        GAME_JOINED,
        GAME_STARTED,
        GAME_ERROR,
        
        // Battle messages
        BATTLE_STATE_UPDATE,
        PLAYER_MOVE,
        SWITCH_POKEMON,
        FORFEIT,
        TURN_COMPLETE,
        BATTLE_END,
        
        // Error messages
        ERROR,
        INVALID_MOVE
    }
    
    /**
     * Base class for all network messages
     */
    public static abstract class Message implements Serializable {
        private static final long serialVersionUID = 1L;
        private final MessageType type;
        private final long timestamp;
        
        public Message(MessageType type) {
            this.type = type;
            this.timestamp = System.currentTimeMillis();
        }
        
        public MessageType getType() {
            return type;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
    
    /**
     * Connection request message
     */
    public static class ConnectMessage extends Message {
        private static final long serialVersionUID = 1L;
        private final String username;
        private final String version;
        
        public ConnectMessage(String username, String version) {
            super(MessageType.CONNECT);
            this.username = username;
            this.version = version;
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getVersion() {
            return version;
        }
    }
    
    /**
     * Create game request
     */
    public static class CreateGameMessage extends Message {
        private static final long serialVersionUID = 1L;
        private final String playerName;
        private final List<PokemonDTO> team;
        
        public CreateGameMessage(String playerName, List<PokemonDTO> team) {
            super(MessageType.CREATE_GAME);
            this.playerName = playerName;
            this.team = team;
        }
        
        public String getPlayerName() {
            return playerName;
        }
        
        public List<PokemonDTO> getTeam() {
            return team;
        }
    }
    
    /**
     * Game created response
     */
    public static class GameCreatedMessage extends Message {
        private static final long serialVersionUID = 1L;
        private final String gameId;
        private final boolean isPlayerOne;
        
        public GameCreatedMessage(String gameId, boolean isPlayerOne) {
            super(MessageType.GAME_CREATED);
            this.gameId = gameId;
            this.isPlayerOne = isPlayerOne;
        }
        
        public String getGameId() {
            return gameId;
        }
        
        public boolean isPlayerOne() {
            return isPlayerOne;
        }
    }
    
    /**
     * Join game request
     */
    public static class JoinGameMessage extends Message {
        private static final long serialVersionUID = 1L;
        private final String gameId;
        private final String playerName;
        private final List<PokemonDTO> team;
        
        public JoinGameMessage(String gameId, String playerName, List<PokemonDTO> team) {
            super(MessageType.JOIN_GAME);
            this.gameId = gameId;
            this.playerName = playerName;
            this.team = team;
        }
        
        public String getGameId() {
            return gameId;
        }
        
        public String getPlayerName() {
            return playerName;
        }
        
        public List<PokemonDTO> getTeam() {
            return team;
        }
    }
    
    /**
     * Game joined response
     */
    public static class GameJoinedMessage extends Message {
        private static final long serialVersionUID = 1L;
        private final String gameId;
        private final boolean isPlayerOne;
        private final String opponentName;
        
        public GameJoinedMessage(String gameId, boolean isPlayerOne, String opponentName) {
            super(MessageType.GAME_JOINED);
            this.gameId = gameId;
            this.isPlayerOne = isPlayerOne;
            this.opponentName = opponentName;
        }
        
        public String getGameId() {
            return gameId;
        }
        
        public boolean isPlayerOne() {
            return isPlayerOne;
        }
        
        public String getOpponentName() {
            return opponentName;
        }
    }
    
    /**
     * Game started notification
     */
    public static class GameStartedMessage extends Message {
        private static final long serialVersionUID = 1L;
        private final BattleStateDTO initialState;
        
        public GameStartedMessage(BattleStateDTO initialState) {
            super(MessageType.GAME_STARTED);
            this.initialState = initialState;
        }
        
        public BattleStateDTO getInitialState() {
            return initialState;
        }
    }
    
    /**
     * Battle state update
     */
    public static class BattleStateUpdateMessage extends Message {
        private static final long serialVersionUID = 1L;
        private final BattleStateDTO state;
        private final String actionMessage;
        
        public BattleStateUpdateMessage(BattleStateDTO state, String actionMessage) {
            super(MessageType.BATTLE_STATE_UPDATE);
            this.state = state;
            this.actionMessage = actionMessage;
        }
        
        public BattleStateDTO getState() {
            return state;
        }
        
        public String getActionMessage() {
            return actionMessage;
        }
    }
    
    /**
     * Player move action
     */
    public static class PlayerMoveMessage extends Message {
        private static final long serialVersionUID = 1L;
        private final int moveIndex;
        
        public PlayerMoveMessage(int moveIndex) {
            super(MessageType.PLAYER_MOVE);
            this.moveIndex = moveIndex;
        }
        
        public int getMoveIndex() {
            return moveIndex;
        }
    }
    
    /**
     * Switch Pokemon action
     */
    public static class SwitchPokemonMessage extends Message {
        private static final long serialVersionUID = 1L;
        private final int pokemonIndex;
        
        public SwitchPokemonMessage(int pokemonIndex) {
            super(MessageType.SWITCH_POKEMON);
            this.pokemonIndex = pokemonIndex;
        }
        
        public int getPokemonIndex() {
            return pokemonIndex;
        }
    }

    /**
     * Forfeit action
     */
    public static class ForfeitMessage extends Message {
        private static final long serialVersionUID = 1L;
        private final String reason;

        public ForfeitMessage(String reason) {
            super(MessageType.FORFEIT);
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }
    }
    
    /**
     * Turn complete notification
     */
    public static class TurnCompleteMessage extends Message {
        private static final long serialVersionUID = 1L;
        
        public TurnCompleteMessage() {
            super(MessageType.TURN_COMPLETE);
        }
    }
    
    /**
     * Battle end notification
     */
    public static class BattleEndMessage extends Message {
        private static final long serialVersionUID = 1L;
        private final boolean playerOneWon;
        private final String winnerName;
        private final String loserName;
        private final BattleOutcomeType outcomeType;

        public BattleEndMessage(boolean playerOneWon, String winnerName, String loserName, BattleOutcomeType outcomeType) {
            super(MessageType.BATTLE_END);
            this.playerOneWon = playerOneWon;
            this.winnerName = winnerName;
            this.loserName = loserName;
            this.outcomeType = outcomeType;
        }

        public boolean isPlayerOneWon() {
            return playerOneWon;
        }

        public String getWinnerName() {
            return winnerName;
        }

        public String getLoserName() {
            return loserName;
        }

        public BattleOutcomeType getOutcomeType() {
            return outcomeType;
        }
    }
    
    /**
     * Error message
     */
    public static class ErrorMessage extends Message {
        private static final long serialVersionUID = 1L;
        private final String errorCode;
        private final String errorMessage;
        
        public ErrorMessage(String errorCode, String errorMessage) {
            super(MessageType.ERROR);
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
    
    /**
     * Game error message
     */
    public static class GameErrorMessage extends Message {
        private static final long serialVersionUID = 1L;
        private final String error;
        
        public GameErrorMessage(String error) {
            super(MessageType.GAME_ERROR);
            this.error = error;
        }
        
        public String getError() {
            return error;
        }
    }
    
    /**
     * Disconnect message
     */
    public static class DisconnectMessage extends Message {
        private static final long serialVersionUID = 1L;
        private final String reason;
        
        public DisconnectMessage(String reason) {
            super(MessageType.DISCONNECT);
            this.reason = reason;
        }
        
        public String getReason() {
            return reason;
        }
    }
    
    /**
     * Heartbeat message for connection keep-alive
     */
    public static class HeartbeatMessage extends Message {
        private static final long serialVersionUID = 1L;
        
        public HeartbeatMessage() {
            super(MessageType.HEARTBEAT);
        }
    }
    
    // Error codes
    public static final String ERROR_GAME_NOT_FOUND = "GAME_NOT_FOUND";
    public static final String ERROR_GAME_FULL = "GAME_FULL";
    public static final String ERROR_INVALID_MOVE = "INVALID_MOVE";
    public static final String ERROR_NOT_YOUR_TURN = "NOT_YOUR_TURN";
    public static final String ERROR_CONNECTION_LOST = "CONNECTION_LOST";
    public static final String ERROR_PROTOCOL_VERSION = "PROTOCOL_VERSION";
    public static final String ERROR_INVALID_TEAM = "INVALID_TEAM";
    
    // Protocol version
    public static final String PROTOCOL_VERSION = "1.0";
}
