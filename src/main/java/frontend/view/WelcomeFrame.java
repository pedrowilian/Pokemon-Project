package frontend.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import frontend.util.UIUtils;
import shared.util.I18n;

/**
 * Welcome screen that displays when the application starts.
 * Allows the user to select their preferred language before proceeding to login.
 */
public class WelcomeFrame extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(WelcomeFrame.class.getName());
    private static final String LOGO_PATH = "Images/pokemon-logo.png";
    private static final String POKEBALL_PATH = "Images/poke-ball.png";

    private JButton startButton;
    private JLabel welcomeLabel;
    private JLabel instructionLabel;
    private JLabel languageLabel;

    public WelcomeFrame() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Pokémon Game");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        
        // Add window listener for exit confirmation
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int confirm = JOptionPane.showConfirmDialog(
                    WelcomeFrame.this,
                    I18n.get("common.confirm.exit"),
                    I18n.get("common.confirm.exit.title"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }
        });

        // Create main panel with gradient background
        JPanel mainPanel = new GradientPanel();
        mainPanel.setLayout(new BorderLayout(20, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        // Create center panel with content
        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // Add logo
        addLogo(centerPanel);

        // Add spacing
        centerPanel.add(Box.createRigidArea(new Dimension(0, 30)));

        // Add welcome text
        addWelcomeText(centerPanel);

        // Add spacing
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Add instruction
        addInstruction(centerPanel);

        // Add spacing
        centerPanel.add(Box.createRigidArea(new Dimension(0, 40)));

        // Add start button
        addStartButton(centerPanel);

        // Add spacing
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Add language info
        addLanguageInfo(centerPanel);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Add decorative pokeballs
        addDecorations(mainPanel);

        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(null);

        LOGGER.log(Level.INFO, "WelcomeFrame initialized");
    }

    private void addLogo(JPanel panel) {
        try {
            ImageIcon logoIcon = new ImageIcon(LOGO_PATH);
            Image logoImage = logoIcon.getImage().getScaledInstance(400, 150, Image.SCALE_SMOOTH);
            JLabel logoLabel = new JLabel(new ImageIcon(logoImage));
            logoLabel.setAlignmentX(CENTER_ALIGNMENT);
            panel.add(logoLabel);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load Pokemon logo", e);
            // Fallback to text
            JLabel titleLabel = new JLabel("POKÉMON GAME");
            titleLabel.setFont(new Font("Arial", Font.BOLD, 48));
            titleLabel.setForeground(UIUtils.PRIMARY_COLOR);
            titleLabel.setAlignmentX(CENTER_ALIGNMENT);
            panel.add(titleLabel);
        }
    }

    private void addWelcomeText(JPanel panel) {
        welcomeLabel = new JLabel(I18n.get("welcome.title"));
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 32));
        welcomeLabel.setForeground(UIUtils.PRIMARY_COLOR);
        welcomeLabel.setAlignmentX(CENTER_ALIGNMENT);
        panel.add(welcomeLabel);
    }

    private void addInstruction(JPanel panel) {
        instructionLabel = new JLabel(I18n.get("welcome.instruction"));
        instructionLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        instructionLabel.setForeground(Color.DARK_GRAY);
        instructionLabel.setAlignmentX(CENTER_ALIGNMENT);
        panel.add(instructionLabel);
    }

    private void addStartButton(JPanel panel) {
        startButton = new JButton(I18n.get("welcome.button.start"));
        startButton.setFont(new Font("Arial", Font.BOLD, 24));
        startButton.setPreferredSize(new Dimension(300, 60));
        startButton.setMaximumSize(new Dimension(300, 60));
        startButton.setBackground(UIUtils.PRIMARY_COLOR);
        startButton.setForeground(Color.WHITE);
        startButton.setFocusPainted(false);
        startButton.setBorderPainted(false);
        startButton.setOpaque(true);
        startButton.setAlignmentX(CENTER_ALIGNMENT);
        startButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        // Hover effect
        startButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                startButton.setBackground(UIUtils.ACCENT_COLOR);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                startButton.setBackground(UIUtils.PRIMARY_COLOR);
            }
        });

        startButton.addActionListener(e -> showLanguageMenu());

        panel.add(startButton);
    }

    private void addLanguageInfo(JPanel panel) {
        languageLabel = new JLabel(I18n.get("welcome.currentLanguage",
            I18n.getCurrentLocale().getDisplayLanguage(I18n.getCurrentLocale())));
        languageLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        languageLabel.setForeground(Color.GRAY);
        languageLabel.setAlignmentX(CENTER_ALIGNMENT);
        panel.add(languageLabel);
    }

    private void addDecorations(JPanel mainPanel) {
        try {
            // Left decoration
            JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            leftPanel.setOpaque(false);
            ImageIcon leftIcon = new ImageIcon(POKEBALL_PATH);
            Image leftImage = leftIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
            JLabel leftLabel = new JLabel(new ImageIcon(leftImage));
            leftPanel.add(leftLabel);
            mainPanel.add(leftPanel, BorderLayout.WEST);

            // Right decoration
            JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            rightPanel.setOpaque(false);
            ImageIcon rightIcon = new ImageIcon(POKEBALL_PATH);
            Image rightImage = rightIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
            JLabel rightLabel = new JLabel(new ImageIcon(rightImage));
            rightPanel.add(rightLabel);
            mainPanel.add(rightPanel, BorderLayout.EAST);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load decoration images", e);
        }
    }

    private void showLanguageMenu() {
        JPopupMenu languageMenu = new JPopupMenu();
        languageMenu.setBorder(BorderFactory.createLineBorder(UIUtils.PRIMARY_COLOR, 2));

        Locale[] availableLocales = I18n.getAvailableLocales();

        for (Locale locale : availableLocales) {
            String displayName = locale.getDisplayLanguage(locale);
            // Capitalize first letter
            displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);

            JMenuItem menuItem = new JMenuItem(displayName);
            menuItem.setFont(new Font("Arial", Font.PLAIN, 16));
            menuItem.setIcon(createFlagIcon(locale));

            menuItem.addActionListener(e -> {
                selectLanguage(locale);
                languageMenu.setVisible(false);
            });

            languageMenu.add(menuItem);
        }

        // Show popup menu below the start button
        languageMenu.show(startButton, 0, startButton.getHeight());
    }

    private ImageIcon createFlagIcon(Locale locale) {
        // Create a simple colored circle as a flag placeholder
        int size = 20;
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Different colors for different locales
        Color color = switch (locale.getLanguage()) {
            case "pt" -> new Color(0, 155, 58); // Green (Brazil flag)
            case "en" -> new Color(60, 59, 110); // Blue (US/UK flag)
            case "es" -> new Color(198, 11, 30); // Red (Spain flag)
            default -> UIUtils.PRIMARY_COLOR;
        };

        g2d.setColor(color);
        g2d.fillOval(0, 0, size, size);
        g2d.dispose();

        return new ImageIcon(image);
    }

    private void selectLanguage(Locale locale) {
        LOGGER.log(Level.INFO, "User selected language: {0}", locale.getDisplayName());

        // Change the locale
        I18n.setLocale(locale);

        // Update UI texts
        updateTexts();

        // After a short delay, proceed to login
        new Thread(() -> {
            try {
                Thread.sleep(500); // Small delay to show the language change
                SwingUtilities.invokeLater(this::proceedToLogin);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void updateTexts() {
        welcomeLabel.setText(I18n.get("welcome.title"));
        instructionLabel.setText(I18n.get("welcome.instruction"));
        startButton.setText(I18n.get("welcome.button.start"));
        languageLabel.setText(I18n.get("welcome.currentLanguage",
            I18n.getCurrentLocale().getDisplayLanguage(I18n.getCurrentLocale())));
    }

    private void proceedToLogin() {
        LOGGER.log(Level.INFO, "Proceeding to login screen");
        this.dispose();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    /**
     * Custom panel with gradient background
     */
    private static class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Create gradient from top to bottom
            GradientPaint gradient = new GradientPaint(
                0, 0, new Color(240, 248, 255), // Light blue at top
                0, getHeight(), new Color(255, 250, 240) // Light cream at bottom
            );

            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}
