package app;

import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import backend.infrastructure.network.BattleServer;

/**
 * Main class to start the Pokemon Battle Server
 */
public class ServerMain {
    private static final Logger LOGGER = Logger.getLogger(ServerMain.class.getName());
    private static final int DEFAULT_PORT = 8888;
    
    public static void main(String[] args) {
        printBanner();
        
        int port = DEFAULT_PORT;
        
        // Check if port was provided as argument
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
                if (port < 1024 || port > 65535) {
                    System.err.println("⚠️  Invalid port number. Using default: " + DEFAULT_PORT);
                    port = DEFAULT_PORT;
                }
            } catch (NumberFormatException e) {
                System.err.println("⚠️  Invalid port format. Using default: " + DEFAULT_PORT);
                port = DEFAULT_PORT;
            }
        }
        
        BattleServer server = new BattleServer(port);
        
        try {
            server.start();
            System.out.println("🎮 Pokemon Battle Server is running on port " + port);
            System.out.println("📡 Server Address: localhost:" + port);
            System.out.println("🔗 For LAN play, use your IP address instead of localhost");
            System.out.println("\n💡 Commands:");
            System.out.println("   'status' - Show server status");
            System.out.println("   'help'   - Show available commands");
            System.out.println("   'stop'   - Stop the server");
            System.out.println("\nServer is ready! Waiting for connections...\n");
            
            // Keep server running and accept commands
            handleCommands(server);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start server", e);
            System.err.println("❌ Error: " + e.getMessage());
            System.exit(1);
        }
    }
    
    /**
     * Handle server commands from console
     */
    private static void handleCommands(BattleServer server) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        
        while (running) {
            try {
                String command = scanner.nextLine().trim().toLowerCase();
                
                switch (command) {
                    case "stop":
                    case "exit":
                    case "quit":
                        System.out.println("\n🛑 Stopping server...");
                        server.stop();
                        running = false;
                        System.out.println("✅ Server stopped successfully!");
                        break;
                        
                    case "status":
                        System.out.println("\n📊 Server Status:");
                        System.out.println("   Status: ✅ Running");
                        System.out.println("   Port: " + DEFAULT_PORT);
                        System.out.println("   Ready for connections");
                        System.out.println();
                        break;
                        
                    case "help":
                        System.out.println("\n📖 Available Commands:");
                        System.out.println("   status - Show server status");
                        System.out.println("   help   - Show this help message");
                        System.out.println("   stop   - Stop the server and exit");
                        System.out.println();
                        break;
                        
                    case "":
                        // Ignore empty input
                        break;
                        
                    default:
                        System.out.println("⚠️  Unknown command: '" + command + "'. Type 'help' for available commands.");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error processing command", e);
            }
        }
        
        scanner.close();
    }
    
    /**
     * Print server banner
     */
    private static void printBanner() {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                            ║");
        System.out.println("║           🎮  POKEMON BATTLE SERVER  🎮                    ║");
        System.out.println("║                                                            ║");
        System.out.println("║              Multiplayer Battle System                     ║");
        System.out.println("║                   Version 1.0                              ║");
        System.out.println("║                                                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
    }
}
