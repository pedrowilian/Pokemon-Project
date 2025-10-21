package backend.network.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Constantes e utilitários para o protocolo de rede
 */
public class NetworkProtocol {
    
    // Porta padrão do servidor
    public static final int DEFAULT_PORT = 5555;
    
    // Timeout de conexão (30 segundos)
    public static final int CONNECTION_TIMEOUT = 30000;
    
    // Nomes de serviços
    public static final String SERVICE_USER = "UserService";
    public static final String SERVICE_POKEMON = "PokemonService";
    public static final String SERVICE_TEAM = "TeamService";
    public static final String SERVICE_BATTLE = "BattleService";
    
    // Métodos UserService
    public static final String METHOD_LOGIN = "login";
    public static final String METHOD_REGISTER = "register";
    public static final String METHOD_UPDATE_USER = "updateUser";
    public static final String METHOD_DELETE_USER = "deleteUser";
    public static final String METHOD_GET_ALL_USERS = "getAllUsers";
    public static final String METHOD_VALIDATE_USERNAME = "validateUsername";
    public static final String METHOD_VALIDATE_PASSWORD = "validatePassword";
    
    // Métodos PokemonService
    public static final String METHOD_GET_ALL_POKEMON = "getAllPokemon";
    public static final String METHOD_SEARCH_POKEMON = "searchPokemon";
    public static final String METHOD_GET_ALL_TYPES = "getAllTypes";
    public static final String METHOD_GET_ATTRIBUTE_MAX_VALUES = "getAttributeMaxValues";
    
    // Métodos TeamService
    public static final String METHOD_SAVE_TEAM = "saveTeam";
    public static final String METHOD_GET_USER_TEAMS = "getUserTeams";
    
    // Gson instance para serialização JSON
    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .create();
    
    /**
     * Cria uma instância de Gson configurada
     */
    public static Gson createGson() {
        return new GsonBuilder()
                .serializeNulls()
                .create();
    }
    
    /**
     * Retorna a instância compartilhada de Gson
     */
    public static Gson getGson() {
        return GSON;
    }
    
    /**
     * Converte objeto para JSON
     */
    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }
    
    /**
     * Converte JSON para objeto
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }
    
    /**
     * Converte JSON para NetworkRequest
     */
    public static NetworkRequest parseRequest(String json) {
        return GSON.fromJson(json, NetworkRequest.class);
    }
    
    /**
     * Converte JSON para NetworkResponse
     */
    public static NetworkResponse parseResponse(String json) {
        return GSON.fromJson(json, NetworkResponse.class);
    }
}
