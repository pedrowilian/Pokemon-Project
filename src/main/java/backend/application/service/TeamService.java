package backend.application.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import backend.domain.model.Pokemon;
import backend.domain.model.Team;
import backend.domain.service.IPokemonRepository;

/**
 * Team service - handles team generation and validation
 * Extracted from TeamSelectionPanel
 */
public class TeamService {
    private static final int MAX_TEAM_SIZE = 5;
    private static final Random random = new Random();

    private final IPokemonRepository pokemonRepository;

    public TeamService(IPokemonRepository pokemonRepository) {
        this.pokemonRepository = pokemonRepository;
    }

    /**
     * Generate a random enemy team
     * Uses the same algorithm from TeamSelectionPanel
     */
    public Team generateRandomTeam(String trainerName) throws SQLException {
        List<Pokemon> allPokemon = pokemonRepository.findAll();
        List<Pokemon> selectedPokemon = new ArrayList<>();
        List<Integer> usedIndices = new ArrayList<>();

        // Select 5 random Pokemon
        while (selectedPokemon.size() < MAX_TEAM_SIZE && usedIndices.size() < allPokemon.size()) {
            int randomIndex = random.nextInt(allPokemon.size());

            if (!usedIndices.contains(randomIndex)) {
                usedIndices.add(randomIndex);
                selectedPokemon.add(allPokemon.get(randomIndex));
            }
        }

        return new Team(trainerName, selectedPokemon);
    }

    /**
     * Create a team from a list of Pokemon
     */
    public Team createTeam(String trainerName, List<Pokemon> pokemon) {
        if (pokemon.size() > MAX_TEAM_SIZE) {
            throw new IllegalArgumentException("Team size cannot exceed " + MAX_TEAM_SIZE);
        }
        return new Team(trainerName, pokemon);
    }

    /**
     * Validate team size
     */
    public boolean isValidTeamSize(int size) {
        return size > 0 && size <= MAX_TEAM_SIZE;
    }

    /**
     * Get maximum team size
     */
    public static int getMaxTeamSize() {
        return MAX_TEAM_SIZE;
    }
}
