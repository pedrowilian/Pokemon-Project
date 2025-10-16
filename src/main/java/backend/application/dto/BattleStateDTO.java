package backend.application.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Data Transfer Object for Battle State
 * Used for network communication - can sync battle across client/server
 */
public class BattleStateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<PokemonDTO> playerTeam;
    private List<PokemonDTO> enemyTeam;
    private int playerActivePokemonIndex;
    private int enemyActivePokemonIndex;
    private String currentTurn;
    private String phase;
    private String lastActionMessage;
    private boolean battleEnded;
    private String winner;

    public BattleStateDTO() {
    }

    // Getters and Setters
    public List<PokemonDTO> getPlayerTeam() {
        return playerTeam;
    }

    public void setPlayerTeam(List<PokemonDTO> playerTeam) {
        this.playerTeam = playerTeam;
    }

    public List<PokemonDTO> getEnemyTeam() {
        return enemyTeam;
    }

    public void setEnemyTeam(List<PokemonDTO> enemyTeam) {
        this.enemyTeam = enemyTeam;
    }

    public int getPlayerActivePokemonIndex() {
        return playerActivePokemonIndex;
    }

    public void setPlayerActivePokemonIndex(int playerActivePokemonIndex) {
        this.playerActivePokemonIndex = playerActivePokemonIndex;
    }

    public int getEnemyActivePokemonIndex() {
        return enemyActivePokemonIndex;
    }

    public void setEnemyActivePokemonIndex(int enemyActivePokemonIndex) {
        this.enemyActivePokemonIndex = enemyActivePokemonIndex;
    }

    public String getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(String currentTurn) {
        this.currentTurn = currentTurn;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getLastActionMessage() {
        return lastActionMessage;
    }

    public void setLastActionMessage(String lastActionMessage) {
        this.lastActionMessage = lastActionMessage;
    }

    public boolean isBattleEnded() {
        return battleEnded;
    }

    public void setBattleEnded(boolean battleEnded) {
        this.battleEnded = battleEnded;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }
}
