package frontend.infrastructure;

import frontend.service.IBattleService;
import frontend.service.IPokemonService;
import frontend.service.ITeamService;
import frontend.service.IUserService;
import frontend.service.impl.LocalBattleService;
import frontend.service.impl.LocalPokemonService;
import frontend.service.impl.LocalTeamService;
import frontend.service.impl.LocalUserService;

/**
 * Service Locator for Frontend
 * Provides access to service implementations
 * 
 * IMPORTANT: This is the ONLY place in frontend that should access backend
 * All other frontend classes should use this locator
 * 
 * Advantages:
 * 1. Single point to switch between local and remote implementations
 * 2. Frontend code doesn't know about backend implementation details
 * 3. Easy to mock services for testing
 * 
 * Usage in Frontend:
 * ```java
 * IUserService userService = FrontendServiceLocator.getInstance().getUserService();
 * userService.authenticate(username, password);
 * ```
 * 
 * Future client-server:
 * Just change the implementations here:
 * ```java
 * this.userService = new RemoteUserService(serverConnection);
 * ```
 */
public class FrontendServiceLocator {
    private static FrontendServiceLocator instance;
    
    private final IUserService userService;
    private final IPokemonService pokemonService;
    private final IBattleService battleService;
    private final ITeamService teamService;
    
    private FrontendServiceLocator() {
        // Initialize with local implementations
        // In client-server mode, these would be Remote implementations
        this.userService = new LocalUserService();
        this.pokemonService = new LocalPokemonService();
        this.battleService = new LocalBattleService();
        this.teamService = new LocalTeamService();
    }
    
    public static synchronized FrontendServiceLocator getInstance() {
        if (instance == null) {
            instance = new FrontendServiceLocator();
        }
        return instance;
    }
    
    public IUserService getUserService() {
        return userService;
    }
    
    public IPokemonService getPokemonService() {
        return pokemonService;
    }
    
    public IBattleService getBattleService() {
        return battleService;
    }
    
    public ITeamService getTeamService() {
        return teamService;
    }
    
    /**
     * Reset instance (useful for testing)
     */
    public static void reset() {
        instance = null;
    }
}
