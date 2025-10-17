package backend.domain.model;

import shared.util.MoveTranslator;

/**
 * Represents a Pokemon move/attack in battle
 */
public class Move {
    private final String name;
    private final String type;
    private final int power;
    private final int accuracy;

    public Move(String name, String type, int power, int accuracy) {
        this.name = name;
        this.type = type;
        this.power = power;
        this.accuracy = accuracy;
    }

    /**
     * Gets the move name in English (as stored in database/backend)
     * @return The move name in English
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the localized move name for UI display
     * @return The translated move name in current locale
     */
    public String getLocalizedName() {
        return MoveTranslator.translate(name);
    }

    public String getType() {
        return type;
    }

    public int getPower() {
        return power;
    }

    public int getAccuracy() {
        return accuracy;
    }

    /**
     * Determines if the move hits based on accuracy
     */
    public boolean hits() {
        return Math.random() * 100 < accuracy;
    }

    @Override
    public String toString() {
        return name + " (" + type + ", Power: " + power + ")";
    }
}
