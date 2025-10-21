package backend.application.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Data Transfer Object for Team
 * Used for network communication and serialization
 */
public class TeamDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String teamName;
    private List<PokemonBattleStatsDTO> pokemons;
    private int activePokemonIndex;

    public TeamDTO() {
    }

    public TeamDTO(String teamName, List<PokemonBattleStatsDTO> pokemons, int activePokemonIndex) {
        this.teamName = teamName;
        this.pokemons = pokemons;
        this.activePokemonIndex = activePokemonIndex;
    }

    // Getters and Setters
    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public List<PokemonBattleStatsDTO> getPokemons() {
        return pokemons;
    }

    public void setPokemons(List<PokemonBattleStatsDTO> pokemons) {
        this.pokemons = pokemons;
    }

    public int getActivePokemonIndex() {
        return activePokemonIndex;
    }

    public void setActivePokemonIndex(int activePokemonIndex) {
        this.activePokemonIndex = activePokemonIndex;
    }
}
