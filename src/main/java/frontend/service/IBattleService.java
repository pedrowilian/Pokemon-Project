package frontend.service;

import java.util.List;

import backend.application.dto.BattleResultDTO;
import backend.application.dto.BattleStateDTO;
import backend.application.dto.MoveDTO;
import backend.application.dto.PokemonDTO;

/**
 * Interface for Battle Service
 * Abstracts battle operations from the frontend
 * Allows future implementation via network (Remote) or local
 */
public interface IBattleService {
    
    /**
     * Start a new battle with two teams
     * Returns the initial battle state
     */
    BattleStateDTO startBattle(List<PokemonDTO> playerTeam, List<PokemonDTO> enemyTeam);
    
    /**
     * Execute a move in battle
     * Returns the result of the move execution
     */
    BattleResultDTO executeMove(String battleId, MoveDTO move);
    
    /**
     * Execute enemy turn (AI)
     * Returns the result of the enemy's move
     */
    BattleResultDTO executeEnemyTurn(String battleId);
    
    /**
     * Switch player Pokemon in battle
     */
    boolean switchPlayerPokemon(String battleId, int pokemonIndex);
    
    /**
     * Switch enemy Pokemon in battle
     */
    boolean switchEnemyPokemon(String battleId, int pokemonIndex);
    
    /**
     * Check if battle should end and update state
     */
    void checkBattleEnd(String battleId);
    
    /**
     * Get current battle state
     */
    BattleStateDTO getBattleState(String battleId);
    
    /**
     * Generate moves for a Pokemon
     */
    List<MoveDTO> generateMovesForPokemon(PokemonDTO pokemon);
    
    /**
     * End battle and clean up resources
     */
    void endBattle(String battleId);
}
