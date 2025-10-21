package frontend.service.impl;

import java.sql.SQLException;
import java.util.List;

import backend.application.dto.DTOMapper;
import backend.application.dto.PokemonDTO;
import backend.application.service.PokemonService;
import backend.domain.model.Pokemon;
import backend.infrastructure.ServiceLocator;
import frontend.service.IPokemonService;

/**
 * Local implementation of Pokemon Service
 * Delegates to backend through ServiceLocator
 * Converts between DTOs and Domain Models
 */
public class LocalPokemonService implements IPokemonService {
    private final PokemonService backendService;
    
    public LocalPokemonService() {
        this.backendService = ServiceLocator.getInstance().getPokemonService();
    }
    
    @Override
    public List<PokemonDTO> getAllPokemon() throws SQLException {
        List<Pokemon> pokemons = backendService.getAllPokemon();
        return DTOMapper.toDTOList(pokemons);
    }
    
    @Override
    public PokemonDTO getPokemonById(int id) throws SQLException {
        Pokemon pokemon = backendService.findById(id);
        return DTOMapper.toDTO(pokemon);
    }
    
    @Override
    public PokemonDTO getPokemonByName(String name) throws SQLException {
        List<Pokemon> pokemons = backendService.searchByName(name);
        if (pokemons.isEmpty()) {
            return null;
        }
        return DTOMapper.toDTO(pokemons.get(0));
    }
    
    @Override
    public List<PokemonDTO> searchPokemon(String nameFilter, String typeFilter,
                                          int minHP, int minAttack, int minDefense,
                                          int minSpAtk, int minSpDef, int minSpeed) throws SQLException {
        // Parse name filter to ID if it's numeric
        Integer searchId = null;
        try {
            searchId = Integer.parseInt(nameFilter);
        } catch (NumberFormatException e) {
            // Not a number, ignore
        }
        
        List<Pokemon> pokemons = backendService.findWithFilters(
            searchId, typeFilter,
            minHP, null,
            minAttack, null,
            minDefense, null,
            minSpAtk, null,
            minSpDef, null,
            minSpeed, null
        );
        
        return DTOMapper.toDTOList(pokemons);
    }
    
    @Override
    public List<PokemonDTO> getRandomPokemon(int count) throws SQLException {
        List<Pokemon> pokemons = backendService.getRandomPokemon(count);
        return DTOMapper.toDTOList(pokemons);
    }
    
    @Override
    public String[] getAllTypes() throws SQLException {
        return backendService.getAllTypes();
    }
    
    @Override
    public IPokemonService.AttributeMaxValues getMaxAttributeValues() throws SQLException {
        backend.domain.service.IPokemonRepository.AttributeMaxValues backendMax = backendService.getMaxAttributeValues();
        return new IPokemonService.AttributeMaxValues(
            backendMax.maxHP(),
            backendMax.maxAttack(),
            backendMax.maxDefense(),
            backendMax.maxSpAtk(),
            backendMax.maxSpDef(),
            backendMax.maxSpeed()
        );
    }
}
