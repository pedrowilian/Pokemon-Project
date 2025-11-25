package backend.application.service;

import java.io.IOException;
import java.io.InputStream;
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
import shared.util.I18n;

/**
 * Battle service - handles all battle logic
 * Extracted from SingleplayerBattlePanel (1,762 lines -> clean service)
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
            String message = I18n.get("battle.backend.missed", attacker.getPokemon().getName(), move.getLocalizedName());
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

        // Build message (without fainted - frontend will handle that)
        String message = I18n.get("battle.backend.attack", attacker.getPokemon().getName(), move.getLocalizedName()) + " ";
        String effectivenessText = TypeEffectiveness.getEffectivenessText(effectiveness);
        if (!effectivenessText.isEmpty()) {
            message += effectivenessText + "! ";
        }
        message += I18n.get("battle.backend.damage", actualDamage);

        // Check if defender fainted (but don't add to message - frontend handles this)
        if (defender.isFainted()) {
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
        // Try classpath resource first (works when packaged)
        try (InputStream is = BattleService.class.getResourceAsStream("/movesData.json")) {
            if (is != null) {
                String content = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                movesStatsData = new JSONObject(content);

                // Iterate through all move names in the JSON object
                for (String moveName : movesStatsData.keySet()) {
                    JSONObject moveData = movesStatsData.getJSONObject(moveName);

                    String type = moveData.getString("type");
                    int power = moveData.optInt("power", 50);
                    int accuracy = moveData.optInt("accuracy", 100);

                    allMoves.add(new Move(moveName, type, power, accuracy));
                }

                LOGGER.log(Level.INFO, "Loaded {0} moves from classpath resource", new Object[]{allMoves.size()});
                return;
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load from classpath: {0}", e.getMessage());
        } catch (JSONException e) {
            LOGGER.log(Level.WARNING, "Failed to parse movesData.json: {0}", e.getMessage());
        }
        
        // Try file system paths (development mode)
        String[] possiblePaths = {
            "movesData.json",
            "./movesData.json",
            "../movesData.json",
            "../../movesData.json"
        };
        
        for (String path : possiblePaths) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(path)));
                movesStatsData = new JSONObject(content);

                // Iterate through all move names in the JSON object
                for (String moveName : movesStatsData.keySet()) {
                    JSONObject moveData = movesStatsData.getJSONObject(moveName);

                    String type = moveData.getString("type");
                    int power = moveData.optInt("power", 50);
                    int accuracy = moveData.optInt("accuracy", 100);

                    allMoves.add(new Move(moveName, type, power, accuracy));
                }

                LOGGER.log(Level.INFO, "Loaded {0} moves from {1}", new Object[]{allMoves.size(), path});
                return;
            } catch (IOException | JSONException e) {
                // Try next path
            }
        }
        
        // FAIL FAST - don't silently fall back to defaults in production
        LOGGER.log(Level.SEVERE, "CRITICAL: Failed to load movesData.json from classpath or filesystem. Multiplayer battles will be broken!");
        addDefaultMoves();
    }

    /**
     * Load Pokemon-specific moves from movesPokemon.json
     * JSON format: { "PokemonName": ["Move1", "Move2", "Move3", "Move4"] }
     */
    private static void loadPokemonMovesFromJSON() {
        // Try classpath resource first (works when packaged)
        try (InputStream is = BattleService.class.getResourceAsStream("/movesPokemon.json")) {
            if (is != null) {
                String content = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                pokemonMovesData = new JSONObject(content);
                LOGGER.log(Level.INFO, "Loaded moves for {0} Pokemon from classpath resource", new Object[]{pokemonMovesData.length()});
                return;
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load from classpath: {0}", e.getMessage());
        } catch (JSONException e) {
            LOGGER.log(Level.WARNING, "Failed to parse movesPokemon.json: {0}", e.getMessage());
        }
        
        // Try file system paths (development mode)
        String[] possiblePaths = {
            "movesPokemon.json",
            "./movesPokemon.json",
            "../movesPokemon.json",
            "../../movesPokemon.json"
        };
        
        for (String path : possiblePaths) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(path)));
                pokemonMovesData = new JSONObject(content);
                LOGGER.log(Level.INFO, "Loaded moves for {0} Pokemon from {1}", new Object[]{pokemonMovesData.length(), path});
                return;
            } catch (IOException | JSONException e) {
                // Try next path
            }
        }
        
        LOGGER.log(Level.SEVERE, "CRITICAL: Failed to load movesPokemon.json from classpath or filesystem. Will use type-based moves as fallback.");
    }

    private static void addDefaultMoves() {
        allMoves.add(new Move("Tackle", "Normal", 40, 100));
        allMoves.add(new Move("Scratch", "Normal", 40, 100));
        allMoves.add(new Move("Ember", "Fire", 40, 100));
        allMoves.add(new Move("Water Gun", "Water", 40, 100));
        allMoves.add(new Move("Vine Whip", "Grass", 45, 100));
    }

    // ========== MULTIPLAYER SUPPORT METHODS ==========

    /**
     * Create a Team from a list of Pokemon
     */
    public Team createTeam(List<Pokemon> pokemonList, String trainerName) {
        return new Team(trainerName, pokemonList);
    }

    /**
     * Get battle state as DTO for network transfer
     */
    public backend.application.dto.BattleStateDTO getBattleStateDTO(BattleState battle) {
        backend.application.dto.BattleStateDTO dto = new backend.application.dto.BattleStateDTO();
        
        // Convert teams to DTOs
        dto.setPlayerTeam(convertTeamToDTO(battle.getPlayerTeam()));
        dto.setEnemyTeam(convertTeamToDTO(battle.getEnemyTeam()));
        
        // Set indices
        dto.setPlayerActivePokemonIndex(battle.getPlayerTeam().getActivePokemonIndex());
        dto.setEnemyActivePokemonIndex(battle.getEnemyTeam().getActivePokemonIndex());
        
        // Set turn and phase
        dto.setCurrentTurn(battle.getCurrentTurn().toString());
        dto.setPhase(battle.getPhase().toString());
        dto.setLastActionMessage(battle.getLastActionMessage());
        
        // Set battle end status
        dto.setBattleEnded(battle.isBattleEnded());
        if (battle.getWinner() != null) {
            dto.setWinner(battle.getWinner().getTrainerName());
        }
        
        return dto;
    }

    /**
     * Convert Team to PokemonDTO list
     */
    private List<backend.application.dto.PokemonDTO> convertTeamToDTO(Team team) {
        List<backend.application.dto.PokemonDTO> dtoList = new ArrayList<>();
        for (PokemonBattleStats stats : team.getAllPokemon()) {
            dtoList.add(convertPokemonToDTO(stats));
        }
        return dtoList;
    }

    /**
     * Convert PokemonBattleStats to DTO
     */
    private backend.application.dto.PokemonDTO convertPokemonToDTO(PokemonBattleStats stats) {
        Pokemon p = stats.getPokemon();
        backend.application.dto.PokemonDTO dto = new backend.application.dto.PokemonDTO();
        
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setType1(p.getType1());
        dto.setType2(p.getType2());
        dto.setHp(p.getHp());
        dto.setAttack(p.getAttack());
        dto.setDefense(p.getDefense());
        dto.setSpAtk(p.getSpAtk());
        dto.setSpDef(p.getSpDef());
        dto.setSpeed(p.getSpeed());
        
        // Battle stats
        dto.setCurrentHp(stats.getCurrentHp());
        dto.setMaxHp(stats.getMaxHp());
        dto.setFainted(stats.isFainted());
        
        // Generate and convert moves for this Pokemon
        List<Move> moves = generateMovesForPokemon(p);
        List<backend.application.dto.MoveDTO> moveDTOs = new ArrayList<>();
        for (Move move : moves) {
            moveDTOs.add(new backend.application.dto.MoveDTO(
                move.getName(), 
                move.getType(), 
                move.getPower(), 
                move.getAccuracy()
            ));
        }
        dto.setAvailableMoves(moveDTOs);
        
        LOGGER.log(Level.INFO, "Generated {0} moves for {1}: {2}", 
            new Object[]{moveDTOs.size(), p.getName(), 
                moveDTOs.stream().map(m -> m.getName()).reduce((a,b) -> a + ", " + b).orElse("none")});
        
        return dto;
    }

    /**
     * Execute a player move by index
     */
    public String executePlayerMove(BattleState battle, int moveIndex) {
        PokemonBattleStats attacker = battle.getActiveTeam().getActivePokemon();
        List<Move> moves = generateMovesForPokemon(attacker.getPokemon());
        
        if (moveIndex < 0 || moveIndex >= moves.size()) {
            throw new IllegalArgumentException("Invalid move index: " + moveIndex);
        }
        
        Move move = moves.get(moveIndex);
        BattleResult result = executeMove(battle, move);
        
        battle.setLastActionMessage(result.getMessage());
        
        // Check if defender fainted
        if (battle.getOpponentTeam().getActivePokemon().isFainted()) {
            String faintMessage = I18n.get("battle.backend.fainted", 
                battle.getOpponentTeam().getActivePokemon().getPokemon().getName());
            battle.setLastActionMessage(result.getMessage() + " " + faintMessage);
        }
        
        // Check battle end
        checkBattleEnd(battle);
        
        // Switch turn if battle not ended
        if (!battle.isBattleEnded()) {
            battle.switchTurn();
        }
        
        return battle.getLastActionMessage();
    }

    /**
     * Switch Pokemon by index (for multiplayer)
     */
    public String switchPokemon(BattleState battle, int pokemonIndex) {
        Team activeTeam = battle.getActiveTeam();
        
        if (pokemonIndex < 0 || pokemonIndex >= activeTeam.getSize()) {
            throw new IllegalArgumentException("Invalid Pokemon index: " + pokemonIndex);
        }
        
        PokemonBattleStats newPokemon = activeTeam.getPokemon(pokemonIndex);
        if (newPokemon.isFainted()) {
            throw new IllegalArgumentException("Cannot switch to fainted Pokemon");
        }
        
        activeTeam.switchPokemon(pokemonIndex);
        String message = activeTeam.getTrainerName() + " switched to " + newPokemon.getPokemon().getName() + "!";
        battle.setLastActionMessage(message);
        
        // Switch turn
        battle.switchTurn();
        
        return message;
    }

    /**
     * Get battle state (returns the domain model directly)
     */
    public BattleState getBattleState(BattleState battle) {
        return battle;
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
