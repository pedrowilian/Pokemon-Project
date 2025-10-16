package backend.domain.service;

import java.sql.SQLException;
import java.util.List;

import backend.domain.model.Pokemon;

/**
 * Repository interface for Pokemon data access
 * Defines contract for Pokemon persistence operations
 */
public interface IPokemonRepository {

    /**
     * Get all Pokemon from the database
     */
    List<Pokemon> findAll() throws SQLException;

    /**
     * Find Pokemon by ID
     */
    Pokemon findById(int id) throws SQLException;

    /**
     * Find Pokemon by name (case-insensitive)
     */
    List<Pokemon> findByName(String name) throws SQLException;

    /**
     * Find Pokemon with dynamic filters
     * @param filters SQL WHERE clause conditions
     * @param params Parameter values for the prepared statement
     */
    List<Pokemon> findWithFilters(String filters, List<Object> params) throws SQLException;

    /**
     * Get random Pokemon
     */
    List<Pokemon> findRandom(int count) throws SQLException;

    /**
     * Get Pokemon by generation
     */
    List<Pokemon> findByGeneration(int generation) throws SQLException;

    /**
     * Get Pokemon by type
     */
    List<Pokemon> findByType(String type) throws SQLException;

    /**
     * Get max attribute values for filtering
     */
    AttributeMaxValues getMaxAttributeValues() throws SQLException;

    /**
     * Get all distinct Pokemon types
     */
    List<String> getAllTypes() throws SQLException;

    /**
     * Record to hold maximum attribute values
     */
    record AttributeMaxValues(int maxHP, int maxAttack, int maxDefense,
                              int maxSpAtk, int maxSpDef, int maxSpeed) {}
}
