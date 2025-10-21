package frontend.service;

import java.sql.SQLException;
import java.util.List;

import backend.application.dto.PokemonDTO;

/**
 * Interface for Team Service
 * Abstracts team operations from the frontend
 * Allows future implementation via network (Remote) or local
 */
public interface ITeamService {
    
    /**
     * Save a team for a user
     */
    boolean saveTeam(String username, List<PokemonDTO> team) throws SQLException;
    
    /**
     * Load team for a user
     */
    List<PokemonDTO> loadTeam(String username) throws SQLException;
    
    /**
     * Check if user has a saved team
     */
    boolean hasTeam(String username) throws SQLException;
    
    /**
     * Delete team for a user
     */
    boolean deleteTeam(String username) throws SQLException;
}
