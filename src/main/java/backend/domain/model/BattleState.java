package backend.domain.model;

/**
 * Represents the current state of a Pokemon battle
 */
public class BattleState {
    private final Team playerTeam;
    private final Team enemyTeam;
    private Turn currentTurn;
    private BattlePhase phase;
    private String lastActionMessage;
    private boolean battleEnded;
    private Team winner;

    public enum Turn {
        PLAYER, ENEMY
    }

    public enum BattlePhase {
        BATTLE_START,
        WAITING_FOR_ACTION,
        EXECUTING_MOVE,
        POKEMON_FAINTED,
        SWITCHING_POKEMON,
        BATTLE_END
    }

    public BattleState(Team playerTeam, Team enemyTeam) {
        this.playerTeam = playerTeam;
        this.enemyTeam = enemyTeam;
        this.currentTurn = Turn.PLAYER;
        this.phase = BattlePhase.BATTLE_START;
        this.lastActionMessage = "";
        this.battleEnded = false;
        this.winner = null;
    }

    public Team getPlayerTeam() {
        return playerTeam;
    }

    public Team getEnemyTeam() {
        return enemyTeam;
    }

    public Turn getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(Turn turn) {
        this.currentTurn = turn;
    }

    public void switchTurn() {
        this.currentTurn = (currentTurn == Turn.PLAYER) ? Turn.ENEMY : Turn.PLAYER;
    }

    public BattlePhase getPhase() {
        return phase;
    }

    public void setPhase(BattlePhase phase) {
        this.phase = phase;
    }

    public String getLastActionMessage() {
        return lastActionMessage;
    }

    public void setLastActionMessage(String message) {
        this.lastActionMessage = message;
    }

    public boolean isBattleEnded() {
        return battleEnded;
    }

    public void setBattleEnded(boolean ended) {
        this.battleEnded = ended;
    }

    public Team getWinner() {
        return winner;
    }

    public void setWinner(Team winner) {
        this.winner = winner;
        this.battleEnded = true;
        this.phase = BattlePhase.BATTLE_END;
    }

    public Team getActiveTeam() {
        return currentTurn == Turn.PLAYER ? playerTeam : enemyTeam;
    }

    public Team getOpponentTeam() {
        return currentTurn == Turn.PLAYER ? enemyTeam : playerTeam;
    }

    /**
     * Check if battle should end and set winner if so
     */
    public void checkBattleEnd() {
        if (playerTeam.isDefeated()) {
            setWinner(enemyTeam);
        } else if (enemyTeam.isDefeated()) {
            setWinner(playerTeam);
        }
    }
}
