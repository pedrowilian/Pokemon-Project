package backend.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a team of Pokemon in battle
 */
public class Team {
    private final List<PokemonBattleStats> pokemon;
    private int activePokemonIndex;
    private final String trainerName;

    public Team(String trainerName, List<Pokemon> pokemonList) {
        this.trainerName = trainerName;
        this.pokemon = new ArrayList<>();
        for (Pokemon p : pokemonList) {
            this.pokemon.add(new PokemonBattleStats(p));
        }
        this.activePokemonIndex = 0;
    }

    public String getTrainerName() {
        return trainerName;
    }

    public PokemonBattleStats getActivePokemon() {
        if (pokemon.isEmpty()) {
            return null;
        }
        return pokemon.get(activePokemonIndex);
    }

    public List<PokemonBattleStats> getAllPokemon() {
        return new ArrayList<>(pokemon);
    }

    public int getActivePokemonIndex() {
        return activePokemonIndex;
    }

    /**
     * Switch to a different Pokemon
     * @param index Index of the Pokemon to switch to
     * @return true if switch was successful
     */
    public boolean switchPokemon(int index) {
        if (index < 0 || index >= pokemon.size()) {
            return false;
        }
        if (pokemon.get(index).isFainted()) {
            return false;
        }
        activePokemonIndex = index;
        return true;
    }

    /**
     * Automatically switch to first non-fainted Pokemon
     * @return true if a Pokemon was found, false if all fainted
     */
    public boolean autoSwitchToNextAlive() {
        for (int i = 0; i < pokemon.size(); i++) {
            if (!pokemon.get(i).isFainted()) {
                activePokemonIndex = i;
                return true;
            }
        }
        return false;
    }

    /**
     * Check if team has been defeated (all Pokemon fainted)
     */
    public boolean isDefeated() {
        return pokemon.stream().allMatch(PokemonBattleStats::isFainted);
    }

    /**
     * Get count of alive Pokemon
     */
    public int getAlivePokemonCount() {
        return (int) pokemon.stream().filter(p -> !p.isFainted()).count();
    }

    /**
     * Get team size
     */
    public int getSize() {
        return pokemon.size();
    }

    /**
     * Get Pokemon at specific index
     */
    public PokemonBattleStats getPokemon(int index) {
        if (index < 0 || index >= pokemon.size()) {
            return null;
        }
        return pokemon.get(index);
    }
}
