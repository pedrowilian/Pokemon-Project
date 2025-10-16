package backend.application.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
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
    private static final List<Move> allMoves = new ArrayList<>();
    private static JSONObject pokemonMovesData = new JSONObject();
    private static JSONObject movesStatsData = new JSONObject();

    static {
        loadMovesFromJSON();
        loadPokemonMovesFromJSON();
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
        message += "Causou " + actualDamage + " de dano! ";

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
     * Generate moves for a Pokemon
     * First tries to load from movesPokemon.json, then falls back to type-based moves
     */
    public List<Move> generateMovesForPokemon(Pokemon pokemon) {
        List<Move> pokemonMoves = new ArrayList<>();

        // Try to get Pokemon-specific moves from movesPokemon.json
        try {
            if (pokemonMovesData.has(pokemon.getName())) {
                org.json.JSONArray moveNames = pokemonMovesData.getJSONArray(pokemon.getName());

                // Extract all moves from the JSON array (up to 4)
                for (int i = 0; i < Math.min(4, moveNames.length()); i++) {
                    String moveName = moveNames.getString(i);
                    Move move = createMoveFromName(moveName);
                    pokemonMoves.add(move);
                }

                // If we got moves from JSON, return them
                if (!pokemonMoves.isEmpty()) {
                    LOGGER.log(Level.INFO, "Loaded {0} moves for {1} from movesPokemon.json",
                              new Object[]{pokemonMoves.size(), pokemon.getName()});
                    return pokemonMoves;
                }
            }
        } catch (JSONException e) {
            LOGGER.log(Level.WARNING, "Error loading moves for {0}, using type-based moves", pokemon.getName());
        }

        // Fallback: Generate moves based on Pokemon's types
        LOGGER.log(Level.INFO, "Generating type-based moves for {0}", pokemon.getName());

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

        // Final fallback: if still no moves, add default moves
        if (pokemonMoves.isEmpty()) {
            LOGGER.log(Level.WARNING, "No moves found for {0}, using default Tackle", pokemon.getName());
            pokemonMoves.add(new Move("Tackle", "Normal", 40, 100));
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
     * Create a Move object from move name, looking up stats from movesStatsData
     * Falls back to default values if move not found
     */
    private static Move createMoveFromName(String moveName) {
        try {
            if (movesStatsData.has(moveName)) {
                JSONObject moveData = movesStatsData.getJSONObject(moveName);
                String type = moveData.optString("type", "Normal");
                int power = moveData.optInt("power", 50);
                int accuracy = moveData.optInt("accuracy", 100);
                return new Move(moveName, type, power, accuracy);
            } else {
                // Move not found in JSON, use default values
                LOGGER.log(Level.WARNING, "Move ''{0}'' not found in movesData.json, using defaults", moveName);
                return new Move(moveName, "Normal", 50, 100);
            }
        } catch (JSONException e) {
            LOGGER.log(Level.WARNING, "Error parsing move data for ''{0}'', using defaults", moveName);
            return new Move(moveName, "Normal", 50, 100);
        }
    }

    /**
     * Load moves from JSON file
     * JSON format is an object with move names as keys
     */
    private static void loadMovesFromJSON() {
        try {
            String content = new String(Files.readAllBytes(Paths.get("movesData.json")));
            movesStatsData = new JSONObject(content);

            // Iterate through all move names in the JSON object
            for (String moveName : movesStatsData.keySet()) {
                JSONObject moveData = movesStatsData.getJSONObject(moveName);

                String type = moveData.getString("type");
                int power = moveData.optInt("power", 50);
                int accuracy = moveData.optInt("accuracy", 100);

                allMoves.add(new Move(moveName, type, power, accuracy));
            }

            LOGGER.log(Level.INFO, "Loaded {0} moves from movesData.json", allMoves.size());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load moves from JSON", e);
            addDefaultMoves();
        } catch (JSONException e) {
            LOGGER.log(Level.SEVERE, "Error parsing moves JSON", e);
            addDefaultMoves();
        }
    }

    /**
     * Load Pokemon-specific moves from movesPokemon.json
     * JSON format: { "PokemonName": ["Move1", "Move2", "Move3", "Move4"] }
     */
    private static void loadPokemonMovesFromJSON() {
        try {
            String content = new String(Files.readAllBytes(Paths.get("movesPokemon.json")));
            pokemonMovesData = new JSONObject(content);
            LOGGER.log(Level.INFO, "Loaded moves for {0} Pokemon from movesPokemon.json", pokemonMovesData.length());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load movesPokemon.json, will use type-based moves", e);
        } catch (JSONException e) {
            LOGGER.log(Level.WARNING, "Error parsing movesPokemon.json, will use type-based moves", e);
        }
    }

    private static void addDefaultMoves() {
        allMoves.add(new Move("Tackle", "Normal", 40, 100));
        allMoves.add(new Move("Scratch", "Normal", 40, 100));
        allMoves.add(new Move("Ember", "Fire", 40, 100));
        allMoves.add(new Move("Water Gun", "Water", 40, 100));
        allMoves.add(new Move("Vine Whip", "Grass", 45, 100));
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
