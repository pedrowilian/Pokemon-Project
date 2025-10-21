package backend.application.dto;

import java.io.Serializable;

/**
 * Data Transfer Object for Move
 * Used for network communication and serialization
 */
public class MoveDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String type;
    private String category;
    private int power;
    private int accuracy;
    private int pp;

    public MoveDTO() {
    }

    public MoveDTO(String name, String type, String category, int power, int accuracy, int pp) {
        this.name = name;
        this.type = type;
        this.category = category;
        this.power = power;
        this.accuracy = accuracy;
        this.pp = pp;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public int getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(int accuracy) {
        this.accuracy = accuracy;
    }

    public int getPp() {
        return pp;
    }

    public void setPp(int pp) {
        this.pp = pp;
    }
}
