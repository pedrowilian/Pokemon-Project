package backend.infrastructure;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import backend.application.service.BattleService;
import backend.application.service.PokemonService;
import backend.application.service.TeamService;
import backend.application.service.UserService;
import backend.domain.service.IPokemonRepository;
import backend.domain.service.IUserRepository;
import backend.infrastructure.database.PokemonRepository;
import backend.infrastructure.database.UserRepository;
import backend.infrastructure.persistence.ConnectionManager;

/**
 * Service Locator pattern for dependency injection
 * Manages service instances and their dependencies
 */
public class ServiceLocator {
    private static final Logger LOGGER = Logger.getLogger(ServiceLocator.class.getName());
    private static ServiceLocator instance;

    // Database names
    private static final String USER_DB = "Usuarios.db";
    private static final String POKEDEX_DB = "pokedex.db";

    // Services
    private UserService userService;
    private PokemonService pokemonService;
    private TeamService teamService;
    private BattleService battleService;

    // Repositories
    private IUserRepository userRepository;
    private IPokemonRepository pokemonRepository;

    // Connection Manager
    private final ConnectionManager connectionManager;

    private ServiceLocator() {
        this.connectionManager = ConnectionManager.getInstance();
        try {
            initializeServices();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize services", e);
            throw new RuntimeException("Failed to initialize services", e);
        }
    }

    public static synchronized ServiceLocator getInstance() {
        if (instance == null) {
            instance = new ServiceLocator();
        }
        return instance;
    }

    private void initializeServices() throws SQLException {
        // Initialize repositories
        Connection userConn = connectionManager.getConnection(USER_DB);
        Connection pokedexConn = connectionManager.getConnection(POKEDEX_DB);

        userRepository = new UserRepository(userConn);
        pokemonRepository = new PokemonRepository(pokedexConn);

        // Initialize services
        userService = new UserService(userRepository);
        pokemonService = new PokemonService(pokemonRepository);
        teamService = new TeamService(pokemonRepository);
        battleService = new BattleService();

        LOGGER.log(Level.INFO, "All services initialized successfully");
    }

    public UserService getUserService() {
        return userService;
    }

    public PokemonService getPokemonService() {
        return pokemonService;
    }

    public TeamService getTeamService() {
        return teamService;
    }

    public BattleService getBattleService() {
        return battleService;
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public IUserRepository getUserRepository() {
        return userRepository;
    }

    public IPokemonRepository getPokemonRepository() {
        return pokemonRepository;
    }

    /**
     * Cleanup all resources
     */
    public void shutdown() {
        connectionManager.closeAll();
        LOGGER.log(Level.INFO, "ServiceLocator shutdown complete");
    }

    /**
     * Reset instance (useful for testing)
     */
    public static synchronized void reset() {
        if (instance != null) {
            instance.shutdown();
            instance = null;
        }
    }
}
