package backend.domain.model;

/**
 * Enriched Pokemon model with battle-specific behavior and state
 * Wraps the original Pokemon model and adds battle logic
 */
public class PokemonBattleStats {
    private final Pokemon pokemon;
    private int currentHp;
    private boolean isFainted;

    public PokemonBattleStats(Pokemon pokemon) {
        this.pokemon = pokemon;
        this.currentHp = pokemon.getHp();
        this.isFainted = false;
    }

    public Pokemon getPokemon() {
        return pokemon;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public int getMaxHp() {
        return pokemon.getHp();
    }

    public boolean isFainted() {
        return isFainted;
    }

    /**
     * Apply damage to this Pokemon
     * @param damage Amount of damage to apply
     * @return Actual damage dealt
     */
    public int takeDamage(int damage) {
        if (isFainted) {
            return 0;
        }

        int actualDamage = Math.min(damage, currentHp);
        currentHp -= actualDamage;

        if (currentHp <= 0) {
            currentHp = 0;
            isFainted = true;
        }

        return actualDamage;
    }

    /**
     * Heal this Pokemon
     * @param amount Amount to heal
     */
    public void heal(int amount) {
        if (!isFainted) {
            currentHp = Math.min(currentHp + amount, pokemon.getHp());
        }
    }

    /**
     * Fully restore this Pokemon
     */
    public void fullRestore() {
        currentHp = pokemon.getHp();
        isFainted = false;
    }

    /**
     * Calculate damage this Pokemon would deal with a move against a target
     */
    public int calculateDamage(Move move, PokemonBattleStats target) {
        if (move == null || target == null) {
            return 0;
        }

        // Base damage calculation
        double baseDamage = move.getPower();

        // Attack/Defense ratio (simplified formula)
        double attackStat = pokemon.getAttack();
        double defenseStat = target.getPokemon().getDefense();

        // Type effectiveness
        double effectiveness = TypeEffectiveness.getTotalEffectiveness(
            move.getType(),
            target.getPokemon().getType1(),
            target.getPokemon().getType2()
        );

        // STAB (Same Type Attack Bonus)
        boolean stab = move.getType().equals(pokemon.getType1()) ||
                      move.getType().equals(pokemon.getType2());
        double stabMultiplier = stab ? 1.5 : 1.0;

        // Random factor (85-100%)
        double randomFactor = 0.85 + (Math.random() * 0.15);

        // Final damage calculation
        double damage = ((2.0 * 50.0 / 5.0 + 2.0) * baseDamage * (attackStat / defenseStat) / 50.0 + 2.0)
                       * effectiveness * stabMultiplier * randomFactor;

        return Math.max(1, (int) damage);
    }

    /**
     * Get HP percentage
     */
    public double getHpPercentage() {
        return (double) currentHp / pokemon.getHp();
    }

    @Override
    public String toString() {
        return pokemon.getName() + " (" + currentHp + "/" + pokemon.getHp() + " HP)";
    }
}
