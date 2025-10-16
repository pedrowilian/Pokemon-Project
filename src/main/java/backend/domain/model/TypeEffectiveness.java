package backend.domain.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates Pokemon type effectiveness chart
 * Handles damage multipliers based on attacking type vs defending type
 */
public class TypeEffectiveness {
    private static final Map<String, Map<String, Double>> EFFECTIVENESS_CHART = new HashMap<>();

    static {
        initializeChart();
    }

    private static void initializeChart() {
        // Normal
        addEffectiveness("Normal", "Rock", 0.5);
        addEffectiveness("Normal", "Ghost", 0.0);
        addEffectiveness("Normal", "Steel", 0.5);

        // Fire
        addEffectiveness("Fire", "Fire", 0.5);
        addEffectiveness("Fire", "Water", 0.5);
        addEffectiveness("Fire", "Grass", 2.0);
        addEffectiveness("Fire", "Ice", 2.0);
        addEffectiveness("Fire", "Bug", 2.0);
        addEffectiveness("Fire", "Rock", 0.5);
        addEffectiveness("Fire", "Dragon", 0.5);
        addEffectiveness("Fire", "Steel", 2.0);

        // Water
        addEffectiveness("Water", "Fire", 2.0);
        addEffectiveness("Water", "Water", 0.5);
        addEffectiveness("Water", "Grass", 0.5);
        addEffectiveness("Water", "Ground", 2.0);
        addEffectiveness("Water", "Rock", 2.0);
        addEffectiveness("Water", "Dragon", 0.5);

        // Electric
        addEffectiveness("Electric", "Water", 2.0);
        addEffectiveness("Electric", "Electric", 0.5);
        addEffectiveness("Electric", "Grass", 0.5);
        addEffectiveness("Electric", "Ground", 0.0);
        addEffectiveness("Electric", "Flying", 2.0);
        addEffectiveness("Electric", "Dragon", 0.5);

        // Grass
        addEffectiveness("Grass", "Fire", 0.5);
        addEffectiveness("Grass", "Water", 2.0);
        addEffectiveness("Grass", "Grass", 0.5);
        addEffectiveness("Grass", "Poison", 0.5);
        addEffectiveness("Grass", "Ground", 2.0);
        addEffectiveness("Grass", "Flying", 0.5);
        addEffectiveness("Grass", "Bug", 0.5);
        addEffectiveness("Grass", "Rock", 2.0);
        addEffectiveness("Grass", "Dragon", 0.5);
        addEffectiveness("Grass", "Steel", 0.5);

        // Ice
        addEffectiveness("Ice", "Fire", 0.5);
        addEffectiveness("Ice", "Water", 0.5);
        addEffectiveness("Ice", "Grass", 2.0);
        addEffectiveness("Ice", "Ice", 0.5);
        addEffectiveness("Ice", "Ground", 2.0);
        addEffectiveness("Ice", "Flying", 2.0);
        addEffectiveness("Ice", "Dragon", 2.0);
        addEffectiveness("Ice", "Steel", 0.5);

        // Fighting
        addEffectiveness("Fighting", "Normal", 2.0);
        addEffectiveness("Fighting", "Ice", 2.0);
        addEffectiveness("Fighting", "Poison", 0.5);
        addEffectiveness("Fighting", "Flying", 0.5);
        addEffectiveness("Fighting", "Psychic", 0.5);
        addEffectiveness("Fighting", "Bug", 0.5);
        addEffectiveness("Fighting", "Rock", 2.0);
        addEffectiveness("Fighting", "Ghost", 0.0);
        addEffectiveness("Fighting", "Dark", 2.0);
        addEffectiveness("Fighting", "Steel", 2.0);
        addEffectiveness("Fighting", "Fairy", 0.5);

        // Poison
        addEffectiveness("Poison", "Grass", 2.0);
        addEffectiveness("Poison", "Poison", 0.5);
        addEffectiveness("Poison", "Ground", 0.5);
        addEffectiveness("Poison", "Rock", 0.5);
        addEffectiveness("Poison", "Ghost", 0.5);
        addEffectiveness("Poison", "Steel", 0.0);
        addEffectiveness("Poison", "Fairy", 2.0);

        // Ground
        addEffectiveness("Ground", "Fire", 2.0);
        addEffectiveness("Ground", "Electric", 2.0);
        addEffectiveness("Ground", "Grass", 0.5);
        addEffectiveness("Ground", "Poison", 2.0);
        addEffectiveness("Ground", "Flying", 0.0);
        addEffectiveness("Ground", "Bug", 0.5);
        addEffectiveness("Ground", "Rock", 2.0);
        addEffectiveness("Ground", "Steel", 2.0);

        // Flying
        addEffectiveness("Flying", "Electric", 0.5);
        addEffectiveness("Flying", "Grass", 2.0);
        addEffectiveness("Flying", "Fighting", 2.0);
        addEffectiveness("Flying", "Bug", 2.0);
        addEffectiveness("Flying", "Rock", 0.5);
        addEffectiveness("Flying", "Steel", 0.5);

        // Psychic
        addEffectiveness("Psychic", "Fighting", 2.0);
        addEffectiveness("Psychic", "Poison", 2.0);
        addEffectiveness("Psychic", "Psychic", 0.5);
        addEffectiveness("Psychic", "Dark", 0.0);
        addEffectiveness("Psychic", "Steel", 0.5);

        // Bug
        addEffectiveness("Bug", "Fire", 0.5);
        addEffectiveness("Bug", "Grass", 2.0);
        addEffectiveness("Bug", "Fighting", 0.5);
        addEffectiveness("Bug", "Poison", 0.5);
        addEffectiveness("Bug", "Flying", 0.5);
        addEffectiveness("Bug", "Psychic", 2.0);
        addEffectiveness("Bug", "Ghost", 0.5);
        addEffectiveness("Bug", "Dark", 2.0);
        addEffectiveness("Bug", "Steel", 0.5);
        addEffectiveness("Bug", "Fairy", 0.5);

        // Rock
        addEffectiveness("Rock", "Fire", 2.0);
        addEffectiveness("Rock", "Ice", 2.0);
        addEffectiveness("Rock", "Fighting", 0.5);
        addEffectiveness("Rock", "Ground", 0.5);
        addEffectiveness("Rock", "Flying", 2.0);
        addEffectiveness("Rock", "Bug", 2.0);
        addEffectiveness("Rock", "Steel", 0.5);

        // Ghost
        addEffectiveness("Ghost", "Normal", 0.0);
        addEffectiveness("Ghost", "Psychic", 2.0);
        addEffectiveness("Ghost", "Ghost", 2.0);
        addEffectiveness("Ghost", "Dark", 0.5);

        // Dragon
        addEffectiveness("Dragon", "Dragon", 2.0);
        addEffectiveness("Dragon", "Steel", 0.5);
        addEffectiveness("Dragon", "Fairy", 0.0);

        // Dark
        addEffectiveness("Dark", "Fighting", 0.5);
        addEffectiveness("Dark", "Psychic", 2.0);
        addEffectiveness("Dark", "Ghost", 2.0);
        addEffectiveness("Dark", "Dark", 0.5);
        addEffectiveness("Dark", "Fairy", 0.5);

        // Steel
        addEffectiveness("Steel", "Fire", 0.5);
        addEffectiveness("Steel", "Water", 0.5);
        addEffectiveness("Steel", "Electric", 0.5);
        addEffectiveness("Steel", "Ice", 2.0);
        addEffectiveness("Steel", "Rock", 2.0);
        addEffectiveness("Steel", "Steel", 0.5);
        addEffectiveness("Steel", "Fairy", 2.0);

        // Fairy
        addEffectiveness("Fairy", "Fire", 0.5);
        addEffectiveness("Fairy", "Fighting", 2.0);
        addEffectiveness("Fairy", "Poison", 0.5);
        addEffectiveness("Fairy", "Dragon", 2.0);
        addEffectiveness("Fairy", "Dark", 2.0);
        addEffectiveness("Fairy", "Steel", 0.5);
    }

    private static void addEffectiveness(String attackType, String defendType, double multiplier) {
        EFFECTIVENESS_CHART.computeIfAbsent(attackType, k -> new HashMap<>()).put(defendType, multiplier);
    }

    /**
     * Gets the type effectiveness multiplier for an attack type against a defending type
     * @param attackType The type of the attacking move
     * @param defendType The type of the defending Pokemon
     * @return Multiplier (0.0, 0.5, 1.0, or 2.0)
     */
    public static double getMultiplier(String attackType, String defendType) {
        if (attackType == null || defendType == null) {
            return 1.0;
        }

        Map<String, Double> typeMap = EFFECTIVENESS_CHART.get(attackType);
        if (typeMap == null) {
            return 1.0;
        }

        return typeMap.getOrDefault(defendType, 1.0);
    }

    /**
     * Gets total effectiveness against a Pokemon with one or two types
     */
    public static double getTotalEffectiveness(String attackType, String defendType1, String defendType2) {
        double multiplier = getMultiplier(attackType, defendType1);

        if (defendType2 != null && !defendType2.isEmpty()) {
            multiplier *= getMultiplier(attackType, defendType2);
        }

        return multiplier;
    }

    /**
     * Gets a descriptive text for the effectiveness
     */
    public static String getEffectivenessText(double multiplier) {
        if (multiplier == 0.0) {
            return "não tem efeito";
        } else if (multiplier < 1.0) {
            return "não é muito efetivo";
        } else if (multiplier > 1.0) {
            return "é super efetivo";
        }
        return "";
    }
}
