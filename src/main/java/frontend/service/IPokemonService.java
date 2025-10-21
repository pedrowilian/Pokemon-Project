package frontend.service;

import java.sql.SQLException;
import java.util.List;

import backend.application.dto.PokemonDTO;

/**
 * Interface for Pokemon Service
 * Abstracts Pokemon operations from the frontend
 * Allows future implementation via network (Remote) or local
 */
public interface IPokemonService {
    
    /**
     * Get all Pokemon
     */
    List<PokemonDTO> getAllPokemon() throws SQLException;
    
    /**
     * Get Pokemon by ID
     */
    PokemonDTO getPokemonById(int id) throws SQLException;
    
    /**
     * Get Pokemon by name
     */
    PokemonDTO getPokemonByName(String name) throws SQLException;
    
    /**
     * Search Pokemon with filters
     */
    List<PokemonDTO> searchPokemon(String nameFilter, String typeFilter, 
                                   int minHP, int minAttack, int minDefense, 
                                   int minSpAtk, int minSpDef, int minSpeed) throws SQLException;
    
    /**
     * Get random Pokemon
     */
    List<PokemonDTO> getRandomPokemon(int count) throws SQLException;
    
    /**
     * Get all Pokemon types
     */
    String[] getAllTypes() throws SQLException;
    
    /**
     * Get maximum attribute values (for UI sliders)
     */
    AttributeMaxValues getMaxAttributeValues() throws SQLException;
    
    /**
     * Container for maximum attribute values
     */
    public static class AttributeMaxValues {
        public final int maxHP;
        public final int maxAttack;
        public final int maxDefense;
        public final int maxSpAtk;
        public final int maxSpDef;
        public final int maxSpeed;
        
        public AttributeMaxValues(int maxHP, int maxAttack, int maxDefense, 
                                 int maxSpAtk, int maxSpDef, int maxSpeed) {
            this.maxHP = maxHP;
            this.maxAttack = maxAttack;
            this.maxDefense = maxDefense;
            this.maxSpAtk = maxSpAtk;
            this.maxSpDef = maxSpDef;
            this.maxSpeed = maxSpeed;
        }
    }
}
