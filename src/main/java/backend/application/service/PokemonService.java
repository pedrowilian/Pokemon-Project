package backend.application.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import backend.domain.model.Pokemon;
import backend.domain.service.IPokemonRepository;
import backend.domain.service.IPokemonRepository.AttributeMaxValues;

/**
 * Pokemon service - handles Pokemon data operations
 * Extracted from GUI classes
 */
public class PokemonService {
    private final IPokemonRepository pokemonRepository;

    public PokemonService(IPokemonRepository pokemonRepository) {
        this.pokemonRepository = pokemonRepository;
    }

    /**
     * Get all Pokemon
     */
    public List<Pokemon> getAllPokemon() throws SQLException {
        return pokemonRepository.findAll();
    }

    /**
     * Find Pokemon by ID
     */
    public Pokemon findById(int id) throws SQLException {
        return pokemonRepository.findById(id);
    }

    /**
     * Search Pokemon by name
     */
    public List<Pokemon> searchByName(String name) throws SQLException {
        return pokemonRepository.findByName(name);
    }

    /**
     * Find Pokemon with filters
     * Builds SQL WHERE clause based on provided filters
     */
    public List<Pokemon> findWithFilters(Integer searchId, String selectedType,
                                         Integer minHP, Integer maxHP,
                                         Integer minAttack, Integer maxAttack,
                                         Integer minDefense, Integer maxDefense,
                                         Integer minSpAtk, Integer maxSpAtk,
                                         Integer minSpDef, Integer maxSpDef,
                                         Integer minSpeed, Integer maxSpeed) throws SQLException {

        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        // ID search
        if (searchId != null) {
            conditions.add("id = ?");
            params.add(searchId);
        }

        // Type filter
        if (selectedType != null && !selectedType.equals("Todos")) {
            conditions.add("(type1 = ? OR type2 = ?)");
            params.add(selectedType);
            params.add(selectedType);
        }

        // Stat range filters
        addRangeFilter(conditions, params, "HP", minHP, maxHP);
        addRangeFilter(conditions, params, "Attack", minAttack, maxAttack);
        addRangeFilter(conditions, params, "Defense", minDefense, maxDefense);
        addRangeFilter(conditions, params, "SpAtk", minSpAtk, maxSpAtk);
        addRangeFilter(conditions, params, "SpDef", minSpDef, maxSpDef);
        addRangeFilter(conditions, params, "Speed", minSpeed, maxSpeed);

        String whereClause = conditions.isEmpty() ? "" : String.join(" AND ", conditions);
        return pokemonRepository.findWithFilters(whereClause, params);
    }

    /**
     * Get max attribute values for UI sliders
     */
    public AttributeMaxValues getMaxAttributeValues() throws SQLException {
        return pokemonRepository.getMaxAttributeValues();
    }

    /**
     * Get all distinct Pokemon types for UI filters
     * Returns list with "All" as first option
     */
    @SuppressWarnings("CollectionsToArray")
    public String[] getAllTypes() throws SQLException {
        List<String> types = pokemonRepository.getAllTypes();
        types.add(0, "All");
        return types.toArray(new String[0]);
    }

    /**
     * Get random Pokemon
     */
    public List<Pokemon> getRandomPokemon(int count) throws SQLException {
        return pokemonRepository.findRandom(count);
    }

    /**
     * Validate Pokemon ID (Generation 1: 1-151)
     */
    public static boolean isValidPokemonId(int id) {
        return id >= 1 && id <= 151;
    }

    /**
     * Helper method to add range filters to SQL query
     */
    private void addRangeFilter(List<String> conditions, List<Object> params,
                               String column, Integer min, Integer max) {
        if (min != null) {
            conditions.add(column + " >= ?");
            params.add(min);
        }
        if (max != null) {
            conditions.add(column + " <= ?");
            params.add(max);
        }
    }
}
