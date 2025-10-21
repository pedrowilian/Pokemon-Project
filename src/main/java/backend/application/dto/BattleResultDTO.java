package backend.application.dto;

import java.io.Serializable;

/**
 * Data Transfer Object for Battle Result
 * Used for network communication and serialization
 */
public class BattleResultDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean hit;
    private int damage;
    private String message;
    private double effectiveness;

    public BattleResultDTO() {
    }

    public BattleResultDTO(boolean hit, int damage, String message, double effectiveness) {
        this.hit = hit;
        this.damage = damage;
        this.message = message;
        this.effectiveness = effectiveness;
    }

    // Getters and Setters
    public boolean isHit() {
        return hit;
    }

    public void setHit(boolean hit) {
        this.hit = hit;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public double getEffectiveness() {
        return effectiveness;
    }

    public void setEffectiveness(double effectiveness) {
        this.effectiveness = effectiveness;
    }
}
