package backend.application.dto;

import java.io.Serializable;

/**
 * Data Transfer Object for Move
 * Used for network communication
 */
public class MoveDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String type;
    private int power;
    private int accuracy;

    public MoveDTO() {
    }

    public MoveDTO(String name, String type, int power, int accuracy) {
        this.name = name;
        this.type = type;
        this.power = power;
        this.accuracy = accuracy;
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
}
