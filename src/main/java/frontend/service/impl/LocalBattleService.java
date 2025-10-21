package frontend.service.impl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import backend.application.dto.BattleResultDTO;
import backend.application.dto.BattleStateDTO;
import backend.application.dto.DTOMapper;
import backend.application.dto.MoveDTO;
import backend.application.dto.PokemonDTO;
import backend.application.service.BattleService;
import backend.domain.model.BattleState;
import backend.domain.model.Move;
import backend.domain.model.Pokemon;
import backend.domain.model.Team;
import backend.infrastructure.ServiceLocator;
import frontend.service.IBattleService;

/**
 * Local implementation of Battle Service
 * Delegates to backend through ServiceLocator
 * Manages battle states in memory
 * Converts between DTOs and Domain Models
 * 
 * This allows frontend to be decoupled from backend domain models
 * In the future, can be replaced with RemoteBattleService for client-server
 */
public class LocalBattleService implements IBattleService {
    private static final Logger LOGGER = Logger.getLogger(LocalBattleService.class.getName());
    
    private final BattleService backendService;
    private final Map<String, BattleState> activeBattles;
    
    public LocalBattleService() {
        this.backendService = ServiceLocator.getInstance().getBattleService();
        this.activeBattles = new ConcurrentHashMap<>();
    }
    
    @Override
    public BattleStateDTO startBattle(List<PokemonDTO> playerTeam, List<PokemonDTO> enemyTeam) {
        try {
            // 1. Convert DTOs to Domain Models
            List<Pokemon> playerPokemons = DTOMapper.toDomainList(playerTeam);
            List<Pokemon> enemyPokemons = DTOMapper.toDomainList(enemyTeam);
            
            // 2. Create Teams
            Team pTeam = new Team("Player", playerPokemons);
            Team eTeam = new Team("Enemy", enemyPokemons);
            
            // 3. Start battle in backend
            BattleState state = backendService.startBattle(pTeam, eTeam);
            
            // 4. Generate unique ID and store state
            String battleId = UUID.randomUUID().toString();
            activeBattles.put(battleId, state);
            
            LOGGER.log(Level.INFO, "Started new battle with ID: {0}", battleId);
            
            // 5. Convert to DTO and return
            BattleStateDTO dto = DTOMapper.toDTO(state);
            // Note: BattleStateDTO doesn't have battleId field, 
            // so frontend will need to manage this separately
            return dto;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error starting battle", e);
            throw new RuntimeException("Failed to start battle: " + e.getMessage(), e);
        }
    }
    
    @Override
    public BattleResultDTO executeMove(String battleId, MoveDTO move) {
        try {
            // 1. Get battle state
            BattleState state = getBattleStateInternal(battleId);
            
            // 2. Convert DTO to Domain Model
            Move domainMove = DTOMapper.toDomain(move);
            
            // 3. Execute move in backend
            BattleService.BattleResult result = backendService.executeMove(state, domainMove);
            
            // 4. Convert result to DTO
            return DTOMapper.toDTO(result);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error executing move in battle " + battleId, e);
            throw new RuntimeException("Failed to execute move: " + e.getMessage(), e);
        }
    }
    
    @Override
    public BattleResultDTO executeEnemyTurn(String battleId) {
        try {
            // 1. Get battle state
            BattleState state = getBattleStateInternal(battleId);
            
            // 2. Execute enemy turn in backend (AI)
            BattleService.BattleResult result = backendService.executeEnemyTurn(state);
            
            // 3. Convert result to DTO
            return DTOMapper.toDTO(result);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error executing enemy turn in battle " + battleId, e);
            throw new RuntimeException("Failed to execute enemy turn: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean switchPlayerPokemon(String battleId, int pokemonIndex) {
        try {
            // 1. Get battle state
            BattleState state = getBattleStateInternal(battleId);
            
            // 2. Switch Pokemon
            boolean success = backendService.switchPlayerPokemon(state, pokemonIndex);
            
            if (success) {
                LOGGER.log(Level.INFO, "Player switched to Pokemon index {0} in battle {1}", 
                          new Object[]{pokemonIndex, battleId});
            }
            
            return success;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error switching player Pokemon in battle " + battleId, e);
            return false;
        }
    }
    
    @Override
    public boolean switchEnemyPokemon(String battleId, int pokemonIndex) {
        try {
            // 1. Get battle state
            BattleState state = getBattleStateInternal(battleId);
            
            // 2. Switch Pokemon
            boolean success = backendService.switchEnemyPokemon(state, pokemonIndex);
            
            if (success) {
                LOGGER.log(Level.INFO, "Enemy switched to Pokemon index {0} in battle {1}", 
                          new Object[]{pokemonIndex, battleId});
            }
            
            return success;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error switching enemy Pokemon in battle " + battleId, e);
            return false;
        }
    }
    
    @Override
    public void checkBattleEnd(String battleId) {
        try {
            // 1. Get battle state
            BattleState state = getBattleStateInternal(battleId);
            
            // 2. Check if battle should end
            backendService.checkBattleEnd(state);
            
            if (state.isBattleEnded()) {
                LOGGER.log(Level.INFO, "Battle {0} has ended", battleId);
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking battle end for battle " + battleId, e);
        }
    }
    
    @Override
    public BattleStateDTO getBattleState(String battleId) {
        try {
            // 1. Get battle state
            BattleState state = getBattleStateInternal(battleId);
            
            // 2. Convert to DTO
            return DTOMapper.toDTO(state);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting battle state for battle " + battleId, e);
            throw new RuntimeException("Failed to get battle state: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<MoveDTO> generateMovesForPokemon(PokemonDTO pokemon) {
        try {
            // 1. Convert DTO to Domain Model
            Pokemon domainPokemon = DTOMapper.toDomain(pokemon);
            
            // 2. Generate moves
            List<Move> moves = backendService.generateMovesForPokemon(domainPokemon);
            
            // 3. Convert to DTOs
            return DTOMapper.toMoveDTOList(moves);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating moves for Pokemon " + pokemon.getName(), e);
            throw new RuntimeException("Failed to generate moves: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void endBattle(String battleId) {
        try {
            // Remove battle from active battles
            BattleState removed = activeBattles.remove(battleId);
            
            if (removed != null) {
                LOGGER.log(Level.INFO, "Ended and removed battle {0}", battleId);
            } else {
                LOGGER.log(Level.WARNING, "Attempted to end non-existent battle {0}", battleId);
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error ending battle " + battleId, e);
        }
    }
    
    /**
     * Internal method to get battle state with proper error handling
     */
    private BattleState getBattleStateInternal(String battleId) {
        if (battleId == null || battleId.trim().isEmpty()) {
            throw new IllegalArgumentException("Battle ID cannot be null or empty");
        }
        
        BattleState state = activeBattles.get(battleId);
        
        if (state == null) {
            throw new IllegalArgumentException("Battle not found with ID: " + battleId);
        }
        
        return state;
    }
    
    /**
     * Get count of active battles (useful for monitoring/debugging)
     */
    public int getActiveBattlesCount() {
        return activeBattles.size();
    }
    
    /**
     * Clear all battles (useful for testing)
     */
    public void clearAllBattles() {
        activeBattles.clear();
        LOGGER.log(Level.INFO, "Cleared all active battles");
    }
}
