package backend.application.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import backend.domain.model.BattleState;
import backend.domain.model.Move;
import backend.domain.model.Pokemon;
import backend.domain.model.PokemonBattleStats;
import backend.domain.model.Team;
import backend.domain.model.User;

/**
 * Mapper utility to convert between Domain Models and DTOs
 * This centralizes the conversion logic and ensures consistency
 */
public class DTOMapper {
    
    // ==================== Pokemon Conversions ====================
    
    public static PokemonDTO toDTO(Pokemon pokemon) {
        if (pokemon == null) return null;
        
        return new PokemonDTO(
            pokemon.getId(),
            pokemon.getName(),
            pokemon.getForm(),
            pokemon.getType1(),
            pokemon.getType2(),
            pokemon.getTotal(),
            pokemon.getHp(),
            pokemon.getAttack(),
            pokemon.getDefense(),
            pokemon.getSpAtk(),
            pokemon.getSpDef(),
            pokemon.getSpeed(),
            pokemon.getGeneration()
        );
    }
    
    public static Pokemon toDomain(PokemonDTO dto) {
        if (dto == null) return null;
        
        return new Pokemon(
            dto.getId(),
            dto.getName(),
            dto.getForm(),
            dto.getType1(),
            dto.getType2(),
            dto.getTotal(),
            dto.getHp(),
            dto.getAttack(),
            dto.getDefense(),
            dto.getSpAtk(),
            dto.getSpDef(),
            dto.getSpeed(),
            dto.getGeneration()
        );
    }
    
    public static List<PokemonDTO> toDTOList(List<Pokemon> pokemons) {
        if (pokemons == null) return new ArrayList<>();
        return pokemons.stream().map(DTOMapper::toDTO).collect(Collectors.toList());
    }
    
    public static List<Pokemon> toDomainList(List<PokemonDTO> dtos) {
        if (dtos == null) return new ArrayList<>();
        return dtos.stream().map(DTOMapper::toDomain).collect(Collectors.toList());
    }
    
    // ==================== User Conversions ====================
    
    public static UserDTO toDTO(User user) {
        if (user == null) return null;
        
        String lastLogin = user.getLastLogin() != null ? user.getLastLoginFormatted() : null;
        String accountCreated = user.getAccountCreated() != null ? user.getAccountCreatedFormatted() : null;
        
        return new UserDTO(
            user.getUsername(),
            user.isAdmin(),
            lastLogin,
            accountCreated
        );
    }
    
    public static User toDomain(UserDTO dto) {
        if (dto == null) return null;
        
        User user = new User(dto.getUsername(), "", dto.isAdmin());
        // Note: password is not in DTO for security reasons
        return user;
    }
    
    public static List<UserDTO> toUserDTOList(List<User> users) {
        if (users == null) return new ArrayList<>();
        return users.stream().map(DTOMapper::toDTO).collect(Collectors.toList());
    }
    
    // ==================== Move Conversions ====================
    
    public static MoveDTO toDTO(Move move) {
        if (move == null) return null;
        
        return new MoveDTO(
            move.getName(),
            move.getType(),
            "Physical", // Category not stored in domain model, default value
            move.getPower(),
            move.getAccuracy(),
            10 // PP not stored in domain model, default value
        );
    }
    
    public static Move toDomain(MoveDTO dto) {
        if (dto == null) return null;
        
        return new Move(
            dto.getName(),
            dto.getType(),
            dto.getPower(),
            dto.getAccuracy()
        );
    }
    
    public static List<MoveDTO> toMoveDTOList(List<Move> moves) {
        if (moves == null) return new ArrayList<>();
        return moves.stream().map(DTOMapper::toDTO).collect(Collectors.toList());
    }
    
    public static List<Move> toMoveDomainList(List<MoveDTO> dtos) {
        if (dtos == null) return new ArrayList<>();
        return dtos.stream().map(DTOMapper::toDomain).collect(Collectors.toList());
    }
    
    // ==================== Battle Stats Conversions ====================
    
    public static PokemonBattleStatsDTO toDTO(PokemonBattleStats stats) {
        if (stats == null) return null;
        
        return new PokemonBattleStatsDTO(
            toDTO(stats.getPokemon()),
            stats.getCurrentHp(),
            stats.getMaxHp(),
            stats.isFainted()
        );
    }
    
    public static List<PokemonBattleStatsDTO> toBattleStatsDTOList(List<PokemonBattleStats> statsList) {
        if (statsList == null) return new ArrayList<>();
        return statsList.stream().map(DTOMapper::toDTO).collect(Collectors.toList());
    }
    
    // ==================== Team Conversions ====================
    
    public static TeamDTO toDTO(Team team) {
        if (team == null) return null;
        
        return new TeamDTO(
            team.getTrainerName(),
            toBattleStatsDTOList(team.getAllPokemon()),
            team.getActivePokemonIndex()
        );
    }
    
    // ==================== Battle State Conversions ====================
    
    public static BattleStateDTO toDTO(BattleState state) {
        if (state == null) return null;
        
        BattleStateDTO dto = new BattleStateDTO();
        dto.setPlayerTeam(toBattleStatsDTOList(state.getPlayerTeam().getAllPokemon()));
        dto.setEnemyTeam(toBattleStatsDTOList(state.getEnemyTeam().getAllPokemon()));
        dto.setPlayerActivePokemonIndex(state.getPlayerTeam().getActivePokemonIndex());
        dto.setEnemyActivePokemonIndex(state.getEnemyTeam().getActivePokemonIndex());
        dto.setCurrentTurn(state.getCurrentTurn().name());
        dto.setPhase(state.getPhase().name());
        dto.setLastActionMessage(state.getLastActionMessage());
        dto.setBattleEnded(state.isBattleEnded());
        dto.setWinner(state.getWinner() != null ? state.getWinner().getTrainerName() : null);
        
        return dto;
    }
    
    // ==================== Battle Result Conversions ====================
    
    public static BattleResultDTO toDTO(backend.application.service.BattleService.BattleResult result) {
        if (result == null) return null;
        
        return new BattleResultDTO(
            result.isHit(),
            result.getDamage(),
            result.getMessage(),
            result.getEffectiveness()
        );
    }
}
