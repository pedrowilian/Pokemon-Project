package frontend.service.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import backend.application.dto.PokemonDTO;
import backend.application.service.TeamService;
import backend.infrastructure.ServiceLocator;
import frontend.service.ITeamService;

/**
 * Local implementation of Team Service
 * Delegates to backend through ServiceLocator
 * Converts between DTOs and Domain Models
 * 
 * Note: Current TeamService in backend doesn't have save/load functionality
 * This implementation provides the interface for future implementation
 */
public class LocalTeamService implements ITeamService {
    private static final Logger LOGGER = Logger.getLogger(LocalTeamService.class.getName());
    
    private final TeamService backendService;
    
    public LocalTeamService() {
        this.backendService = ServiceLocator.getInstance().getTeamService();
    }
    
    @Override
    public boolean saveTeam(String username, List<PokemonDTO> team) throws SQLException {
        try {
            // TODO: Backend TeamService doesn't have saveTeam yet
            // This would need to be implemented in backend first
            // For now, log a warning
            LOGGER.log(Level.WARNING, 
                      "saveTeam called but not implemented in backend. Username: {0}, Team size: {1}", 
                      new Object[]{username, team.size()});
            
            // Temporary: just return true to not break frontend
            // When backend implements this, convert and use:
            // List<Pokemon> pokemons = DTOMapper.toDomainList(team);
            // backendService.saveTeam(username, pokemons);
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving team for user " + username, e);
            throw new SQLException("Failed to save team: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<PokemonDTO> loadTeam(String username) throws SQLException {
        try {
            // TODO: Backend TeamService doesn't have loadTeam yet
            // This would need to be implemented in backend first
            // For now, return null to indicate no saved team
            LOGGER.log(Level.WARNING, 
                      "loadTeam called but not implemented in backend. Username: {0}", 
                      username);
            
            // Temporary: return null
            // When backend implements this, uncomment and convert result:
            // List<Pokemon> pokemons = backendService.loadTeam(username);
            // return DTOMapper.toDTOList(pokemons);
            return null;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading team for user " + username, e);
            throw new SQLException("Failed to load team: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean hasTeam(String username) throws SQLException {
        try {
            // TODO: Backend TeamService doesn't have hasTeam yet
            // For now, always return false
            LOGGER.log(Level.WARNING, 
                      "hasTeam called but not implemented in backend. Username: {0}", 
                      username);
            
            // Temporary: return false
            // When backend implements this, uncomment:
            // return backendService.hasTeam(username);
            return false;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking team for user " + username, e);
            throw new SQLException("Failed to check team: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean deleteTeam(String username) throws SQLException {
        try {
            // TODO: Backend TeamService doesn't have deleteTeam yet
            // For now, log warning and return true
            LOGGER.log(Level.WARNING, 
                      "deleteTeam called but not implemented in backend. Username: {0}", 
                      username);
            
            // Temporary: return true
            // When backend implements this, uncomment:
            // backendService.deleteTeam(username);
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting team for user " + username, e);
            throw new SQLException("Failed to delete team: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate team size (using backend's rules)
     */
    public boolean isValidTeamSize(int size) {
        return backendService.isValidTeamSize(size);
    }
    
    /**
     * Get maximum team size
     */
    public static int getMaxTeamSize() {
        return TeamService.getMaxTeamSize();
    }
}
