package frontend.view;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import backend.application.service.PokemonService;
import backend.domain.model.Pokemon;
import backend.infrastructure.ServiceLocator;
import frontend.util.UIUtils;
import shared.util.I18n;

/**
 * Battle Mode Selection Panel - Choose between Local AI or Multiplayer
 * Modern, polished UI for selecting game mode
 */
public class BattleModeSelectionPanel extends JPanel {
    private static final String ICON_IMAGE_DIR = "Images/Image-Pokedex/";
    
    private final JFrame parentFrame;
    private final String username;
    private final List<Pokemon> selectedTeam;
    private final PokemonService pokemonService;
    
    public BattleModeSelectionPanel(JFrame parentFrame, String username, List<Pokemon> selectedTeam) {
        this.parentFrame = parentFrame;
        this.username = username;
        this.selectedTeam = selectedTeam;
        this.pokemonService = ServiceLocator.getInstance().getPokemonService();
        
        initializeUI();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(240, 245, 255));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createModeSelectionPanel(), BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 10, 20, 10));
        
        // Title
        JLabel titleLabel = new JLabel(I18n.get("mode.title"));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(new Color(200, 0, 0));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Subtitle
        JLabel subtitleLabel = new JLabel(I18n.get("mode.subtitle"));
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        subtitleLabel.setForeground(new Color(80, 80, 80));
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);
        
        panel.add(textPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createModeSelectionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 20, 10, 20);
        
        // Local AI Mode Card
        panel.add(createModeCard(
            I18n.get("mode.local.title"),
            I18n.get("mode.local.description"),
            new Color(76, 175, 80),
            "🤖",
            () -> startLocalBattle()
        ), gbc);
        
        gbc.gridy = 1;
        
        // Multiplayer Mode Card
        panel.add(createModeCard(
            I18n.get("mode.multiplayer.title"),
            I18n.get("mode.multiplayer.description"),
            new Color(33, 150, 243),
            "🌐",
            () -> startMultiplayerSetup()
        ), gbc);
        
        return panel;
    }
    
    private JPanel createModeCard(String title, String description, Color accentColor, String icon, Runnable action) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw rounded background
                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                
                // Draw accent border
                g2d.setColor(accentColor);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 20, 20);
            }
        };
        
        card.setLayout(new BorderLayout(15, 15));
        card.setPreferredSize(new Dimension(600, 150));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));
        card.setOpaque(false);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Icon
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setPreferredSize(new Dimension(80, 80));
        
        // Content
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(50, 50, 50));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel descLabel = new JLabel("<html>" + description + "</html>");
        descLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        descLabel.setForeground(new Color(100, 100, 100));
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(descLabel);
        
        // Arrow
        JLabel arrowLabel = new JLabel("→");
        arrowLabel.setFont(new Font("Arial", Font.BOLD, 36));
        arrowLabel.setForeground(accentColor);
        arrowLabel.setHorizontalAlignment(SwingConstants.CENTER);
        arrowLabel.setPreferredSize(new Dimension(50, 50));
        
        card.add(iconLabel, BorderLayout.WEST);
        card.add(contentPanel, BorderLayout.CENTER);
        card.add(arrowLabel, BorderLayout.EAST);
        
        // Hover effects
        card.addMouseListener(new MouseAdapter() {
            private Color originalBg = card.getBackground();
            
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 20));
                card.repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(originalBg);
                card.repaint();
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        });
        
        return card;
    }
    
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Team preview
        JPanel teamPreview = createTeamPreview();
        
        // Back button
        JButton backButton = UIUtils.createStyledButton(
            I18n.get("mode.button.back"),
            e -> returnToTeamSelection(),
            I18n.get("mode.tooltip.back")
        );
        backButton.setPreferredSize(new Dimension(150, 40));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setOpaque(false);
        buttonPanel.add(backButton);
        
        panel.add(teamPreview, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createTeamPreview() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panel.setOpaque(false);
        
        JLabel label = new JLabel(I18n.get("mode.team.label"));
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setForeground(new Color(80, 80, 80));
        panel.add(label);
        
        // Show first 5 Pokemon icons
        for (int i = 0; i < Math.min(5, selectedTeam.size()); i++) {
            Pokemon pokemon = selectedTeam.get(i);
            JLabel iconLabel = new JLabel(loadPokemonIcon(pokemon.getId(), 40));
            iconLabel.setToolTipText(pokemon.getName());
            panel.add(iconLabel);
        }
        
        return panel;
    }
    
    /**
     * Start local AI battle
     */
    private void startLocalBattle() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Generate AI opponent team with random Pokemon
                List<Pokemon> enemyTeam = pokemonService.getRandomPokemon(5);
                
                parentFrame.getContentPane().removeAll();
                parentFrame.setContentPane(new SingleplayerBattlePanel(
                    selectedTeam,
                    enemyTeam,
                    username,
                    parentFrame
                ));
                parentFrame.revalidate();
                parentFrame.repaint();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    I18n.get("common.error") + ": " + e.getMessage(),
                    I18n.get("common.error"),
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    
    /**
     * Start multiplayer setup
     */
    private void startMultiplayerSetup() {
        SwingUtilities.invokeLater(() -> {
            MultiplayerSetupDialog dialog = new MultiplayerSetupDialog(
                parentFrame,
                username,
                selectedTeam
            );
            dialog.setVisible(true);
        });
    }
    
    /**
     * Return to team selection
     */
    private void returnToTeamSelection() {
        SwingUtilities.invokeLater(() -> {
            parentFrame.getContentPane().removeAll();
            parentFrame.setContentPane(new TeamSelectionPanel(parentFrame, username));
            parentFrame.revalidate();
            parentFrame.repaint();
        });
    }
    
    /**
     * Load Pokemon icon
     */
    private ImageIcon loadPokemonIcon(int id, int size) {
        String file = ICON_IMAGE_DIR + id + ".png";
        File f = new File(file);
        
        if (!f.exists()) {
            return createPlaceholderIcon(size);
        }
        
        ImageIcon icon = new ImageIcon(file);
        Image img = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }
    
    private ImageIcon createPlaceholderIcon(int size) {
        java.awt.image.BufferedImage placeholder = new java.awt.image.BufferedImage(
            size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g2d = placeholder.createGraphics();
        g2d.setColor(Color.GRAY);
        g2d.fillOval(size / 4, size / 4, size / 2, size / 2);
        g2d.dispose();
        return new ImageIcon(placeholder);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Gradient background
        GradientPaint gp = new GradientPaint(
            0, 0, new Color(240, 245, 255),
            0, getHeight(), new Color(200, 220, 255)
        );
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, getWidth(), getHeight());
    }
}
