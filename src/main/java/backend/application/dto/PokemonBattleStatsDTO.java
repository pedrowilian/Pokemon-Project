package backend.application.dto;

import java.io.Serializable;

/**
 * Data Transfer Object for Pokemon Battle Stats
 * Used for network communication and serialization
 */
public class PokemonBattleStatsDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private PokemonDTO pokemon;
    private int currentHP;
    private int maxHP;
    private boolean fainted;

    public PokemonBattleStatsDTO() {
    }

    public PokemonBattleStatsDTO(PokemonDTO pokemon, int currentHP, int maxHP, boolean fainted) {
        this.pokemon = pokemon;
        this.currentHP = currentHP;
        this.maxHP = maxHP;
        this.fainted = fainted;
    }

    // Getters and Setters
    public PokemonDTO getPokemon() {
        return pokemon;
    }

    public void setPokemon(PokemonDTO pokemon) {
        this.pokemon = pokemon;
    }

    public int getCurrentHP() {
        return currentHP;
    }

    public void setCurrentHP(int currentHP) {
        this.currentHP = currentHP;
    }

    public int getMaxHP() {
        return maxHP;
    }

    public void setMaxHP(int maxHP) {
        this.maxHP = maxHP;
    }

    public boolean isFainted() {
        return fainted;
    }

    public void setFainted(boolean fainted) {
        this.fainted = fainted;
    }
}
