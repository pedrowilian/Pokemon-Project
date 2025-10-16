package backend.application.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import backend.domain.model.BattleState;
import backend.domain.model.Move;
import backend.domain.model.Pokemon;
import backend.domain.model.PokemonBattleStats;
import backend.domain.model.Team;
import backend.domain.model.TypeEffectiveness;

/**
 * Battle service - handles all battle logic
 * Extracted from EnhancedBattlePanel (1,762 lines -> clean service)
 */
public class BattleService {
    private static final Logger LOGGER = Logger.getLogger(BattleService.class.getName());
    private static final Random random = new Random();
    private static List<Move> allMoves = new ArrayList<>();

    static {
        loadMovesFromJSON();
    }

    /**
     * Start a new battle
     */
    public BattleState startBattle(Team playerTeam, Team enemyTeam) {
        return new BattleState(playerTeam, enemyTeam);
    }

    /**
     * Execute a move in battle
     */
    public BattleResult executeMove(BattleState battle, Move move) {
        PokemonBattleStats attacker = battle.getActiveTeam().getActivePokemon();
        PokemonBattleStats defender = battle.getOpponentTeam().getActivePokemon();

        // Check if move hits
        if (!move.hits()) {
            String message = attacker.getPokemon().getName() + " usou " + move.getName() + ", mas errou!";
            return new BattleResult(false, 0, message, 1.0);
        }

        // Calculate damage
        int damage = attacker.calculateDamage(move, defender);
        int actualDamage = defender.takeDamage(damage);

        // Get effectiveness
        double effectiveness = TypeEffectiveness.getTotalEffectiveness(
            move.getType(),
            defender.getPokemon().getType1(),
            defender.getPokemon().getType2()
        );

        // Build message
        String message = attacker.getPokemon().getName() + " usou " + move.getName() + "! ";
        String effectivenessText = TypeEffectiveness.getEffectivenessText(effectiveness);
        if (!effectivenessText.isEmpty()) {
            message += "O ataque " + effectivenessText + "! ";
        }
        message += "Causou " + actualDamage + " de dano!";

        // Check if defender fainted
        if (defender.isFainted()) {
            message += "\n" + defender.getPokemon().getName() + " desmaiou!";
            battle.setPhase(BattleState.BattlePhase.POKEMON_FAINTED);
        }

        return new BattleResult(true, actualDamage, message, effectiveness);
    }

    /**
     * Execute enemy turn (AI)
     */
    public BattleResult executeEnemyTurn(BattleState battle) {
        PokemonBattleStats enemyPokemon = battle.getEnemyTeam().getActivePokemon();

        // Generate available moves for enemy Pokemon
        List<Move> enemyMoves = generateMovesForPokemon(enemyPokemon.getPokemon());

        // Simple AI: choose random move
        Move chosenMove = enemyMoves.get(random.nextInt(enemyMoves.size()));

        return executeMove(battle, chosenMove);
    }

    /**
     * Switch player Pokemon in battle
     */
    public boolean switchPlayerPokemon(BattleState battle, int index) {
        return battle.getPlayerTeam().switchPokemon(index);
    }

    /**
     * Switch enemy Pokemon in battle
     */
    public boolean switchEnemyPokemon(BattleState battle, int index) {
        return battle.getEnemyTeam().switchPokemon(index);
    }

    /**
     * Check if battle should end
     */
    public void checkBattleEnd(BattleState battle) {
        battle.checkBattleEnd();
    }

    /**
     * Generate moves for a Pokemon based on its types
     * Reuses logic from EnhancedBattlePanel
     */
    public List<Move> generateMovesForPokemon(Pokemon pokemon) {
        List<Move> pokemonMoves = new ArrayList<>();

        // Add 2 moves of primary type
        List<Move> type1Moves = getMovesOfType(pokemon.getType1());
        for (int i = 0; i < Math.min(2, type1Moves.size()); i++) {
            pokemonMoves.add(type1Moves.get(random.nextInt(type1Moves.size())));
        }

        // Add 1 move of secondary type if exists
        if (pokemon.getType2() != null && !pokemon.getType2().isEmpty()) {
            List<Move> type2Moves = getMovesOfType(pokemon.getType2());
            if (!type2Moves.isEmpty()) {
                pokemonMoves.add(type2Moves.get(random.nextInt(type2Moves.size())));
            }
        }

        // Add 1 random Normal type move if space available
        if (pokemonMoves.size() < 4) {
            List<Move> normalMoves = getMovesOfType("Normal");
            if (!normalMoves.isEmpty()) {
                pokemonMoves.add(normalMoves.get(random.nextInt(normalMoves.size())));
            }
        }

        return pokemonMoves;
    }

    /**
     * Get all moves of a specific type
     */
    private List<Move> getMovesOfType(String type) {
        List<Move> typeMoves = new ArrayList<>();
        for (Move move : allMoves) {
            if (move.getType().equalsIgnoreCase(type)) {
                typeMoves.add(move);
            }
        }
        return typeMoves;
    }

    /**
     * Load moves from JSON file
     * JSON format is an object with move names as keys
     */
    private static void loadMovesFromJSON() {
        try {
            String content = new String(Files.readAllBytes(Paths.get("movesData.json")));
            JSONObject movesObject = new JSONObject(content);

            // Iterate through all move names in the JSON object
            for (String moveName : movesObject.keySet()) {
                JSONObject moveData = movesObject.getJSONObject(moveName);

                String type = moveData.getString("type");
                int power = moveData.optInt("power", 50);
                int accuracy = moveData.optInt("accuracy", 100);

                allMoves.add(new Move(moveName, type, power, accuracy));
            }

            LOGGER.log(Level.INFO, "Loaded " + allMoves.size() + " moves from JSON");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load moves from JSON", e);
            addDefaultMoves();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing moves JSON", e);
            addDefaultMoves();
        }
    }

    private static void addDefaultMoves() {
        allMoves.add(new Move("Tackle", "normal", 40, 100));
        allMoves.add(new Move("Scratch", "normal", 40, 100));
        allMoves.add(new Move("Ember", "fire", 40, 100));
        allMoves.add(new Move("Water Gun", "water", 40, 100));
        allMoves.add(new Move("Vine Whip", "grass", 45, 100));
    }

    /**
     * Battle result data class
     */
    public static class BattleResult {
        private final boolean hit;
        private final int damage;
        private final String message;
        private final double effectiveness;

        public BattleResult(boolean hit, int damage, String message, double effectiveness) {
            this.hit = hit;
            this.damage = damage;
            this.message = message;
            this.effectiveness = effectiveness;
        }

        public boolean isHit() {
            return hit;
        }

        public int getDamage() {
            return damage;
        }

        public String getMessage() {
            return message;
        }

        public double getEffectiveness() {
            return effectiveness;
        }
    }
}
