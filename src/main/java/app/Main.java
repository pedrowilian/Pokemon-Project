package app;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import backend.infrastructure.ServiceLocator;
import frontend.view.LoginFrame;

/**
 * Main application entry point
 * Now uses clean architecture with ServiceLocator for dependency injection
 */
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    @SuppressWarnings("UseSpecificCatch")
    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not set system look and feel", e);
        }

        // Initialize ServiceLocator (loads all services)
        try {
            ServiceLocator.getInstance();
            LOGGER.log(Level.INFO, "Application services initialized successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize application services", e);
            System.exit(1);
        }

        // Add shutdown hook to cleanup resources
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.log(Level.INFO, "Shutting down application...");
            ServiceLocator.getInstance().shutdown();
        }));

        // Launch GUI
        SwingUtilities.invokeLater(() -> {
            try {
                new LoginFrame().setVisible(true);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to launch LoginFrame", e);
            }
        });
    }
}
