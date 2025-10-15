package GUI;

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
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.json.JSONArray;
import org.json.JSONObject;

import model.Pokemon;

public class EnhancedBattlePanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(EnhancedBattlePanel.class.getName());
    private static final String BACK_IMAGE_DIR = "Images/Back-Pokemon-gif/";
    private static final String FRONT_IMAGE_DIR = "Images/Front-Pokemon-gif/";
    private static final String ICON_IMAGE_DIR = "Images/Image-Pokedex/";

    private final List<Pokemon> playerTeam;
    private final List<Pokemon> enemyTeam;
    private final Connection pokedexConn;
    private final Connection usuariosConn;
    private final String username;
    private final JFrame parentFrame;

    private int[] playerCurrentHP;
    private int[] enemyCurrentHP;
    private int[] playerMaxHP;
    private int[] enemyMaxHP;

    private int currentPlayerPokemon = 0;
    private int currentEnemyPokemon = 0;

    private JLabel playerNameLabel, enemyNameLabel;
    private JLabel playerHPLabel, enemyHPLabel;
    private JProgressBar playerHealthBar, enemyHealthBar;
    private JLabel battleMessageLabel;
    private JButton[] attackButtons;
    private JButton switchButton, runButton;
    private JLabel playerSpriteLabel, enemySpriteLabel;
    private JPanel[] playerTeamSlots;
    private JPanel[] enemyTeamSlots;
    private JPanel playerTypePanel, enemyTypePanel;

    private boolean playerTurn = true;
    private boolean battleEnded = false;
    private javax.swing.Timer animationTimer;

    private Attack[] playerAttacks;
    private Attack[] enemyAttacks;

    private JSONObject movesData;
    private JSONObject movesPokemon;

    public EnhancedBattlePanel(List<Pokemon> playerTeam, List<Pokemon> enemyTeam,
                              Connection pokedexConn, Connection usuariosConn,
                              String username, JFrame parentFrame) {
        this.playerTeam = playerTeam;
        this.enemyTeam = enemyTeam;
        this.pokedexConn = pokedexConn;
        this.usuariosConn = usuariosConn;
        this.username = username;
        this.parentFrame = parentFrame;

        playerMaxHP = new int[5];
        enemyMaxHP = new int[5];
        playerCurrentHP = new int[5];
        enemyCurrentHP = new int[5];

        for (int i = 0; i < 5; i++) {
            playerMaxHP[i] = playerTeam.get(i).getHp();
            enemyMaxHP[i] = enemyTeam.get(i).getHp();
            playerCurrentHP[i] = playerMaxHP[i];
            enemyCurrentHP[i] = enemyMaxHP[i];
        }

        loadJSONData();
        loadAttacks();

        playerTurn = playerTeam.get(0).getSpeed() >= enemyTeam.get(0).getSpeed();

        initializeUI();
        startBattle();
    }

    private void loadJSONData() {
        try {
            String movesDataContent = new String(Files.readAllBytes(Paths.get("movesData.json")));
            movesData = new JSONObject(movesDataContent);

            String movesPokemonContent = new String(Files.readAllBytes(Paths.get("movesPokemon.json")));
            movesPokemon = new JSONObject(movesPokemonContent);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading JSON data, using fallback attacks", e);
            movesData = new JSONObject();
            movesPokemon = new JSONObject();
        }
    }

    private void loadAttacks() {
        playerAttacks = generateAttacksFromJSON(playerTeam.get(currentPlayerPokemon));
        enemyAttacks = generateAttacksFromJSON(enemyTeam.get(currentEnemyPokemon));
    }

    private void initializeUI() {
        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(45, 52, 90));
        setDoubleBuffered(true);

        // Add component listener for responsive resizing
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                // Force repaint when window is resized
                revalidate();
                repaint();
            }
        });

        // Main container with BorderLayout
        JPanel mainContainer = new JPanel(new BorderLayout(0, 0));
        mainContainer.setOpaque(false);

        // Left side - Player team vertical bar
        JPanel leftTeamPanel = createVerticalTeamBar(playerTeam, true);
        mainContainer.add(leftTeamPanel, BorderLayout.WEST);

        // Right side - Enemy team vertical bar
        JPanel rightTeamPanel = createVerticalTeamBar(enemyTeam, false);
        mainContainer.add(rightTeamPanel, BorderLayout.EAST);

        // Center - Battle area
        JPanel centerPanel = new JPanel(new BorderLayout(0, 0));
        centerPanel.setOpaque(false);

        // Top - Enemy info
        JPanel enemyInfoPanel = createCompactInfoCard(false);
        centerPanel.add(enemyInfoPanel, BorderLayout.NORTH);

        // Middle - Battle field with sprites
        JPanel battleFieldPanel = createBattleFieldPanel();
        centerPanel.add(battleFieldPanel, BorderLayout.CENTER);

        // Bottom container - Player info and controls
        JPanel bottomContainer = new JPanel(new BorderLayout(0, 10));
        bottomContainer.setOpaque(false);
        bottomContainer.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JPanel playerInfoPanel = createCompactInfoCard(true);
        bottomContainer.add(playerInfoPanel, BorderLayout.NORTH);

        JPanel controlsPanel = createControlPanel();
        bottomContainer.add(controlsPanel, BorderLayout.CENTER);

        centerPanel.add(bottomContainer, BorderLayout.SOUTH);

        mainContainer.add(centerPanel, BorderLayout.CENTER);

        add(mainContainer, BorderLayout.CENTER);
    }

    private JPanel createVerticalTeamBar(List<Pokemon> team, boolean isPlayer) {
        JPanel barPanel = new JPanel();
        barPanel.setLayout(new BoxLayout(barPanel, BoxLayout.Y_AXIS));
        barPanel.setOpaque(true);
        barPanel.setBackground(new Color(0, 0, 0, 200));
        barPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(isPlayer ? new Color(76, 175, 80) : new Color(220, 20, 60), 3),
            BorderFactory.createEmptyBorder(12, 8, 12, 8)
        ));
        barPanel.setPreferredSize(new Dimension(110, 0));

        // Title
        JLabel titleLabel = new JLabel(isPlayer ? "YOUR TEAM" : "ENEMY TEAM", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 11));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        barPanel.add(titleLabel);
        barPanel.add(Box.createVerticalStrut(12));

        JPanel[] slots = new JPanel[5];

        for (int i = 0; i < 5; i++) {
            Pokemon pokemon = team.get(i);
            final int index = i;

            JPanel slotContainer = new JPanel(new BorderLayout(0, 0));
            slotContainer.setOpaque(false);
            slotContainer.setMaximumSize(new Dimension(94, 125));
            slotContainer.setAlignmentX(Component.CENTER_ALIGNMENT);

            JPanel slot = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    // Clear completely first to prevent ghosting
                    g2d.setColor(new Color(0, 0, 0, 0));
                    g2d.setComposite(java.awt.AlphaComposite.Clear);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    // Draw the background
                    g2d.setComposite(java.awt.AlphaComposite.SrcOver);
                    g2d.setColor(getBackground());
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    g2d.dispose();
                }
            };
            slot.setLayout(new BoxLayout(slot, BoxLayout.Y_AXIS));
            slot.setOpaque(false);
            slot.setBackground(new Color(255, 255, 255, 240));
            slot.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60), 2, true),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));

            // Pokemon icon
            JLabel iconLabel = new JLabel();
            iconLabel.setOpaque(false);
            iconLabel.setDoubleBuffered(true);
            iconLabel.setIcon(loadPokemonSmallIcon(pokemon.getId()));
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            slot.add(iconLabel);

            slot.add(Box.createVerticalStrut(4));

            // Pokemon name (shortened)
            String displayName = pokemon.getName().length() > 8 ?
                pokemon.getName().substring(0, 7) + "." : pokemon.getName();
            JLabel nameLabel = new JLabel(displayName, SwingConstants.CENTER);
            nameLabel.setFont(new Font("Arial", Font.BOLD, 9));
            nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            slot.add(nameLabel);

            slot.add(Box.createVerticalStrut(3));

            // HP Bar
            JProgressBar hpBar = new JProgressBar(0, isPlayer ? playerMaxHP[i] : enemyMaxHP[i]);
            hpBar.setValue(isPlayer ? playerCurrentHP[i] : enemyCurrentHP[i]);
            hpBar.setPreferredSize(new Dimension(70, 8));
            hpBar.setMaximumSize(new Dimension(70, 8));
            hpBar.setForeground(new Color(76, 175, 80));
            hpBar.setBackground(new Color(220, 220, 220));
            hpBar.setBorderPainted(true);
            hpBar.setAlignmentX(Component.CENTER_ALIGNMENT);
            slot.add(hpBar);

            // Active indicator (initially hidden)
            JLabel activeIndicator = new JLabel("★ ACTIVE ★", SwingConstants.CENTER);
            activeIndicator.setFont(new Font("Arial", Font.BOLD, 8));
            activeIndicator.setForeground(new Color(255, 215, 0));
            activeIndicator.setAlignmentX(Component.CENTER_ALIGNMENT);
            activeIndicator.setVisible(isPlayer ? i == currentPlayerPokemon : i == currentEnemyPokemon);
            slot.add(Box.createVerticalStrut(2));
            slot.add(activeIndicator);

            slotContainer.add(slot, BorderLayout.CENTER);

            slots[i] = slot;
            barPanel.add(slotContainer);

            if (i < 4) {
                barPanel.add(Box.createVerticalStrut(10));
            }
        }

        if (isPlayer) {
            playerTeamSlots = slots;
        } else {
            enemyTeamSlots = slots;
        }

        barPanel.add(Box.createVerticalGlue());

        return barPanel;
    }

    private JPanel createCompactInfoCard(boolean isPlayer) {
        Pokemon pokemon = isPlayer ? playerTeam.get(currentPlayerPokemon) : enemyTeam.get(currentEnemyPokemon);

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Clear the area first
                g2d.setColor(new Color(0, 0, 0, 0));
                g2d.setComposite(java.awt.AlphaComposite.Clear);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                // Draw the background with proper opacity
                g2d.setComposite(java.awt.AlphaComposite.SrcOver);
                g2d.setColor(new Color(0, 0, 0, 180));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(isPlayer ? new Color(76, 175, 80) : new Color(220, 20, 60), 2, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        card.setMaximumSize(new Dimension(2000, 90));

        // Name and Level row
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        headerPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(pokemon.getName().toUpperCase());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        nameLabel.setForeground(Color.WHITE);
        headerPanel.add(nameLabel);

        JLabel levelLabel = new JLabel(" LV.50 ");
        levelLabel.setFont(new Font("Arial", Font.BOLD, 8));
        levelLabel.setForeground(Color.WHITE);
        levelLabel.setOpaque(true);
        levelLabel.setBackground(isPlayer ? new Color(76, 175, 80) : new Color(220, 20, 60));
        headerPanel.add(levelLabel);

        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(headerPanel);

        if (isPlayer) {
            playerNameLabel = nameLabel;
        } else {
            enemyNameLabel = nameLabel;
        }

        card.add(Box.createVerticalStrut(3));

        // HP Label
        JLabel hpLabel = new JLabel("HP: " + (isPlayer ? playerCurrentHP[currentPlayerPokemon] : enemyCurrentHP[currentEnemyPokemon]) +
                                    " / " + (isPlayer ? playerMaxHP[currentPlayerPokemon] : enemyMaxHP[currentEnemyPokemon]));
        hpLabel.setFont(new Font("Arial", Font.BOLD, 11));
        hpLabel.setForeground(Color.WHITE);
        hpLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(hpLabel);

        if (isPlayer) {
            playerHPLabel = hpLabel;
        } else {
            enemyHPLabel = hpLabel;
        }

        card.add(Box.createVerticalStrut(3));

        // Health Bar
        JProgressBar healthBar = new JProgressBar(0, isPlayer ? playerMaxHP[currentPlayerPokemon] : enemyMaxHP[currentEnemyPokemon]);
        healthBar.setValue(isPlayer ? playerCurrentHP[currentPlayerPokemon] : enemyCurrentHP[currentEnemyPokemon]);
        healthBar.setStringPainted(true);
        healthBar.setString("100%");
        healthBar.setFont(new Font("Arial", Font.BOLD, 9));
        healthBar.setForeground(new Color(76, 175, 80));
        healthBar.setBackground(new Color(80, 80, 80));
        healthBar.setPreferredSize(new Dimension(250, 16));
        healthBar.setMaximumSize(new Dimension(2000, 16));
        healthBar.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        healthBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(healthBar);

        if (isPlayer) {
            playerHealthBar = healthBar;
        } else {
            enemyHealthBar = healthBar;
        }

        card.add(Box.createVerticalStrut(2));

        // Type badges
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        typePanel.setOpaque(false);
        typePanel.add(createTypeBadge(pokemon.getType1()));
        if (pokemon.getType2() != null && !pokemon.getType2().isEmpty() && !pokemon.getType2().equals("None")) {
            typePanel.add(createTypeBadge(pokemon.getType2()));
        }
        typePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(typePanel);

        // Store reference to type panel for updates
        if (isPlayer) {
            playerTypePanel = typePanel;
        } else {
            enemyTypePanel = typePanel;
        }

        return card;
    }

    private JPanel createBattleFieldPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();

        // Enemy sprite (top right area) - HUGE Pokemon!
        enemySpriteLabel = new JLabel();
        enemySpriteLabel.setOpaque(false);
        enemySpriteLabel.setDoubleBuffered(true);
        enemySpriteLabel.setIcon(loadPokemonSprite(enemyTeam.get(currentEnemyPokemon).getId(), true));
        enemySpriteLabel.setHorizontalAlignment(SwingConstants.CENTER);
        enemySpriteLabel.setVerticalAlignment(SwingConstants.CENTER);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.weighty = 0.55;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        gbc.insets = new Insets(10, 20, 0, 30);
        panel.add(enemySpriteLabel, gbc);

        // Player sprite (bottom left area) - HUGE Pokemon!
        playerSpriteLabel = new JLabel();
        playerSpriteLabel.setOpaque(false);
        playerSpriteLabel.setDoubleBuffered(true);
        playerSpriteLabel.setIcon(loadPokemonSprite(playerTeam.get(currentPlayerPokemon).getId(), false));
        playerSpriteLabel.setHorizontalAlignment(SwingConstants.CENTER);
        playerSpriteLabel.setVerticalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 0.45;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        gbc.insets = new Insets(0, 30, 10, 20);
        panel.add(playerSpriteLabel, gbc);

        // Battle message in the center-bottom - enhanced design
        JPanel messagePanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Clear the area completely first
                g2d.setColor(new Color(0, 0, 0, 0));
                g2d.setComposite(java.awt.AlphaComposite.Clear);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Draw a subtle gradient background for depth
                g2d.setComposite(java.awt.AlphaComposite.SrcOver);
                GradientPaint bgGradient = new GradientPaint(
                    0, 0, new Color(20, 20, 20, 240),
                    0, getHeight(), new Color(10, 10, 10, 250)
                );
                g2d.setPaint(bgGradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

                g2d.dispose();
            }
        };
        messagePanel.setOpaque(false);
        messagePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 215, 0), 4, true),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        messagePanel.setPreferredSize(new Dimension(550, 60));
        messagePanel.setMaximumSize(new Dimension(750, 70));

        battleMessageLabel = new JLabel("", SwingConstants.CENTER);
        battleMessageLabel.setFont(new Font("Arial", Font.BOLD, 18));
        battleMessageLabel.setForeground(Color.WHITE);
        battleMessageLabel.setOpaque(false);
        messagePanel.add(battleMessageLabel, BorderLayout.CENTER);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 30, 4, 30);
        panel.add(messagePanel, gbc);

        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(2000, 140));

        // Attack buttons grid - more compact
        JPanel attackPanel = new JPanel(new GridLayout(2, 2, 6, 6));
        attackPanel.setOpaque(false);

        attackButtons = new JButton[4];
        for (int i = 0; i < 4; i++) {
            attackButtons[i] = createAttackButton(playerAttacks[i]);
            attackPanel.add(attackButtons[i]);
        }

        panel.add(attackPanel, BorderLayout.CENTER);

        // Action buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        actionPanel.setOpaque(false);

        switchButton = createActionButton("SWITCH POKEMON", new Color(52, 152, 219));
        switchButton.addActionListener(e -> showSwitchDialog());
        actionPanel.add(switchButton);

        runButton = createActionButton("RUN", new Color(231, 76, 60));
        runButton.addActionListener(e -> endBattle(false));
        actionPanel.add(runButton);

        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JButton createAttackButton(Attack attack) {
        Color typeColor = getTypeColor(attack.type);
        Color disabledColor = new Color(120, 120, 120);

        JButton button = new JButton(String.format(
            "<html><div style='text-align:center'><b>%s</b><br><span style='font-size:8px'>PWR: %d | %s</span></div></html>",
            attack.name, attack.power, attack.type.toUpperCase()));

        button.setFont(new Font("Arial", Font.BOLD, 11));
        button.setBackground(typeColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(typeColor.darker().darker(), 2, true),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(140, 42));
        button.setMinimumSize(new Dimension(100, 36));

        // Store original color as client property
        button.putClientProperty("originalColor", typeColor);
        button.putClientProperty("disabledColor", disabledColor);

        button.addActionListener(e -> executePlayerAttack(attack));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    Color original = (Color) button.getClientProperty("originalColor");
                    button.setBackground(original.brighter());
                    button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.YELLOW, 3, true),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)
                    ));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    Color original = (Color) button.getClientProperty("originalColor");
                    button.setBackground(original);
                    button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(original.darker().darker(), 3, true),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)
                    ));
                } else {
                    // Ensure disabled button stays grey
                    Color disabled = (Color) button.getClientProperty("disabledColor");
                    button.setBackground(disabled);
                }
            }
        });

        return button;
    }

    private JButton createActionButton(String text, Color color) {
        Color disabledColor = new Color(120, 120, 120);

        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 11));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color.darker(), 2, true),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);

        // Store original color as client property
        button.putClientProperty("originalColor", color);
        button.putClientProperty("disabledColor", disabledColor);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    Color original = (Color) button.getClientProperty("originalColor");
                    button.setBackground(original.brighter());
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    Color original = (Color) button.getClientProperty("originalColor");
                    button.setBackground(original);
                } else {
                    // Ensure disabled button stays grey
                    Color disabled = (Color) button.getClientProperty("disabledColor");
                    button.setBackground(disabled);
                }
            }
        });

        return button;
    }

    private void startBattle() {
        SwingUtilities.invokeLater(() -> {
            showBattleMessage("A rival trainer wants to battle!", 2000, () -> {
                showBattleMessage("Rival sent out " + enemyTeam.get(0).getName() + "!", 2000, () -> {
                    showBattleMessage("Go! " + playerTeam.get(0).getName() + "!", 2000, () -> {
                        if (!playerTurn) {
                            showBattleMessage("Enemy " + enemyTeam.get(0).getName() + " will attack first!", 2000, () -> {
                                executeEnemyTurn();
                            });
                        } else {
                            showBattleMessage("What will " + playerTeam.get(0).getName() + " do?", 1000, () -> {
                                enableControls();
                            });
                        }
                    });
                });
            });
        });
    }

    private void showBattleMessage(String message, int duration, Runnable onComplete) {
        battleMessageLabel.setText(message);

        // Force repaint of the message panel to prevent ghosting
        if (battleMessageLabel.getParent() != null) {
            battleMessageLabel.getParent().repaint();
        }

        javax.swing.Timer timer = new javax.swing.Timer(duration, e -> {
            if (onComplete != null) {
                onComplete.run();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void executePlayerAttack(Attack attack) {
        if (battleEnded || !playerTurn) {
            return;
        }

        disableControls();

        Pokemon attacker = playerTeam.get(currentPlayerPokemon);
        Pokemon defender = enemyTeam.get(currentEnemyPokemon);

        showBattleMessage(attacker.getName() + " used " + attack.name + "!", 1500, () -> {
            double effectiveness = getTypeEffectiveness(attack.type, defender.getType1(), defender.getType2());
            int damage = calculateDamage(attacker.getAttack(), attacker.getSpAtk(),
                                        defender.getDefense(), defender.getSpDef(),
                                        attack.power, attack.category, effectiveness);

            animateAttack(true, () -> {
                enemyCurrentHP[currentEnemyPokemon] = Math.max(0, enemyCurrentHP[currentEnemyPokemon] - damage);
                updateHealthBar(false);
                updateTeamSlots();

                String effectMsg = getEffectivenessMessage(effectiveness);

                showBattleMessage("Dealt " + damage + " damage!" + effectMsg, 2000, () -> {
                    if (enemyCurrentHP[currentEnemyPokemon] <= 0) {
                        handlePokemonFainted(false);
                    } else {
                        playerTurn = false;
                        executeEnemyTurn();
                    }
                });
            });
        });
    }

    private void executeEnemyTurn() {
        if (battleEnded) {
            return;
        }

        if (enemyCurrentHP[currentEnemyPokemon] <= 0) {
            switchEnemyPokemon();
            return;
        }

        Random rand = new Random();
        Attack attack = enemyAttacks[rand.nextInt(4)];

        Pokemon attacker = enemyTeam.get(currentEnemyPokemon);
        Pokemon defender = playerTeam.get(currentPlayerPokemon);

        showBattleMessage("Enemy " + attacker.getName() + " used " + attack.name + "!", 1500, () -> {
            double effectiveness = getTypeEffectiveness(attack.type, defender.getType1(), defender.getType2());
            int damage = calculateDamage(attacker.getAttack(), attacker.getSpAtk(),
                                        defender.getDefense(), defender.getSpDef(),
                                        attack.power, attack.category, effectiveness);

            animateAttack(false, () -> {
                playerCurrentHP[currentPlayerPokemon] = Math.max(0, playerCurrentHP[currentPlayerPokemon] - damage);
                updateHealthBar(true);
                updateTeamSlots();

                String effectMsg = getEffectivenessMessage(effectiveness);

                showBattleMessage("Dealt " + damage + " damage!" + effectMsg, 2000, () -> {
                    if (playerCurrentHP[currentPlayerPokemon] <= 0) {
                        handlePokemonFainted(true);
                    } else {
                        playerTurn = true;
                        showBattleMessage("What will " + playerTeam.get(currentPlayerPokemon).getName() + " do?", 1000, () -> {
                            enableControls();
                        });
                    }
                });
            });
        });
    }

    private void handlePokemonFainted(boolean isPlayer) {
        Pokemon fainted = isPlayer ? playerTeam.get(currentPlayerPokemon) : enemyTeam.get(currentEnemyPokemon);

        showBattleMessage(fainted.getName() + " fainted!", 2000, () -> {
            if (isPlayer) {
                boolean hasAlivePokemon = false;
                for (int hp : playerCurrentHP) {
                    if (hp > 0) {
                        hasAlivePokemon = true;
                        break;
                    }
                }

                if (!hasAlivePokemon) {
                    endBattle(false);
                } else {
                    showSwitchDialog();
                }
            } else {
                boolean hasAlivePokemon = false;
                for (int hp : enemyCurrentHP) {
                    if (hp > 0) {
                        hasAlivePokemon = true;
                        break;
                    }
                }

                if (!hasAlivePokemon) {
                    endBattle(true);
                } else {
                    switchEnemyPokemon();
                }
            }
        });
    }

    private void switchPlayerPokemon(int newIndex) {
        if (playerCurrentHP[newIndex] <= 0) {
            JOptionPane.showMessageDialog(this, "That Pokemon has fainted!", "Cannot Switch", JOptionPane.WARNING_MESSAGE);
            return;
        }

        disableControls();

        currentPlayerPokemon = newIndex;

        showBattleMessage(playerTeam.get(currentPlayerPokemon).getName() + ", come back!", 1500, () -> {
            // Clear the old sprite first to prevent ghosting
            playerSpriteLabel.setIcon(null);
            playerSpriteLabel.repaint();

            // Small delay to ensure clean transition
            javax.swing.Timer transitionTimer = new javax.swing.Timer(50, evt -> {
                playerSpriteLabel.setIcon(loadPokemonSprite(playerTeam.get(newIndex).getId(), false));
                playerAttacks = generateAttacksFromJSON(playerTeam.get(newIndex));
                updateAttackButtons();
                updatePlayerInfo();
                updateActiveIndicators();

                // Force repaint of all components
                revalidate();
                repaint();

                showBattleMessage("Go! " + playerTeam.get(newIndex).getName() + "!", 1500, () -> {
                    if (!playerTurn) {
                        executeEnemyTurn();
                    } else {
                        showBattleMessage("What will " + playerTeam.get(newIndex).getName() + " do?", 1000, () -> {
                            enableControls();
                        });
                    }
                });
            });
            transitionTimer.setRepeats(false);
            transitionTimer.start();
        });
    }

    private void switchEnemyPokemon() {
        int newIndex = -1;
        for (int i = 0; i < 5; i++) {
            if (enemyCurrentHP[i] > 0 && i != currentEnemyPokemon) {
                newIndex = i;
                break;
            }
        }

        if (newIndex == -1) {
            endBattle(true);
            return;
        }

        final int finalNewIndex = newIndex;
        currentEnemyPokemon = newIndex;

        showBattleMessage("Rival sent out " + enemyTeam.get(finalNewIndex).getName() + "!", 2000, () -> {
            // Clear the old sprite first to prevent ghosting
            enemySpriteLabel.setIcon(null);
            enemySpriteLabel.repaint();

            // Small delay to ensure clean transition
            javax.swing.Timer transitionTimer = new javax.swing.Timer(50, evt -> {
                enemySpriteLabel.setIcon(loadPokemonSprite(enemyTeam.get(finalNewIndex).getId(), true));
                enemyAttacks = generateAttacksFromJSON(enemyTeam.get(finalNewIndex));
                updateEnemyInfo();
                updateActiveIndicators();

                // Force repaint of all components
                revalidate();
                repaint();

                if (playerTurn) {
                    showBattleMessage("What will " + playerTeam.get(currentPlayerPokemon).getName() + " do?", 1000, () -> {
                        enableControls();
                    });
                } else {
                    executeEnemyTurn();
                }
            });
            transitionTimer.setRepeats(false);
            transitionTimer.start();
        });
    }

    private void updateActiveIndicators() {
        // Update player team active indicators
        for (int i = 0; i < 5; i++) {
            JPanel slot = playerTeamSlots[i];
            Component[] components = slot.getComponents();
            for (Component c : components) {
                if (c instanceof JLabel && ((JLabel) c).getText().contains("ACTIVE")) {
                    c.setVisible(i == currentPlayerPokemon);
                }
            }
        }

        // Update enemy team active indicators
        for (int i = 0; i < 5; i++) {
            JPanel slot = enemyTeamSlots[i];
            Component[] components = slot.getComponents();
            for (Component c : components) {
                if (c instanceof JLabel && ((JLabel) c).getText().contains("ACTIVE")) {
                    c.setVisible(i == currentEnemyPokemon);
                }
            }
        }
    }

    private void showSwitchDialog() {
        JDialog dialog = new JDialog(parentFrame, "Switch Pokemon", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(450, 550);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(new Color(45, 52, 90));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        contentPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Choose a Pokemon to switch to:");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(15));

        for (int i = 0; i < 5; i++) {
            if (i == currentPlayerPokemon) continue;

            final int index = i;
            Pokemon pokemon = playerTeam.get(i);

            JPanel pokemonPanel = new JPanel(new BorderLayout(10, 10));
            pokemonPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(playerCurrentHP[i] > 0 ? new Color(76, 175, 80) : Color.GRAY, 3, true),
                new EmptyBorder(12, 12, 12, 12)
            ));
            pokemonPanel.setBackground(playerCurrentHP[i] > 0 ? new Color(240, 255, 240) : new Color(200, 200, 200));
            pokemonPanel.setMaximumSize(new Dimension(400, 90));

            JLabel iconLabel = new JLabel(loadPokemonSmallIcon(pokemon.getId()));
            pokemonPanel.add(iconLabel, BorderLayout.WEST);

            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setOpaque(false);

            JLabel nameLabel = new JLabel(pokemon.getName());
            nameLabel.setFont(new Font("Arial", Font.BOLD, 16));
            infoPanel.add(nameLabel);

            JLabel hpLabel = new JLabel("HP: " + playerCurrentHP[i] + " / " + playerMaxHP[i]);
            hpLabel.setFont(new Font("Arial", Font.PLAIN, 13));
            infoPanel.add(hpLabel);

            JProgressBar hpBar = new JProgressBar(0, playerMaxHP[i]);
            hpBar.setValue(playerCurrentHP[i]);
            hpBar.setForeground(playerCurrentHP[i] > 0 ? new Color(76, 175, 80) : Color.GRAY);
            hpBar.setPreferredSize(new Dimension(220, 18));
            infoPanel.add(hpBar);

            pokemonPanel.add(infoPanel, BorderLayout.CENTER);

            if (playerCurrentHP[i] > 0) {
                pokemonPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                pokemonPanel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        dialog.dispose();
                        switchPlayerPokemon(index);
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        pokemonPanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(255, 215, 0), 4, true),
                            new EmptyBorder(12, 12, 12, 12)
                        ));
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        pokemonPanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(76, 175, 80), 3, true),
                            new EmptyBorder(12, 12, 12, 12)
                        ));
                    }
                });
            }

            contentPanel.add(pokemonPanel);
            contentPanel.add(Box.createVerticalStrut(10));
        }

        JButton cancelButton = createActionButton("CANCEL", new Color(231, 76, 60));
        cancelButton.addActionListener(e -> dialog.dispose());
        cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(cancelButton);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        dialog.add(scrollPane);

        dialog.setVisible(true);
    }

    private void updateAttackButtons() {
        for (int i = 0; i < 4; i++) {
            Attack attack = playerAttacks[i];
            Color typeColor = getTypeColor(attack.type);

            attackButtons[i].setText(String.format(
                "<html><div style='text-align:center'><b>%s</b><br><span style='font-size:9px'>PWR: %d | %s</span></div></html>",
                attack.name, attack.power, attack.type.toUpperCase()));

            // Update the stored original color
            attackButtons[i].putClientProperty("originalColor", typeColor);

            // Only set background if button is enabled
            if (attackButtons[i].isEnabled()) {
                attackButtons[i].setBackground(typeColor);
                attackButtons[i].setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(typeColor.darker().darker(), 3, true),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
                ));
            }

            final int index = i;
            for (ActionListener al : attackButtons[i].getActionListeners()) {
                attackButtons[i].removeActionListener(al);
            }
            attackButtons[i].addActionListener(e -> executePlayerAttack(playerAttacks[index]));
        }
    }

    private void updatePlayerInfo() {
        Pokemon pokemon = playerTeam.get(currentPlayerPokemon);
        playerNameLabel.setText(pokemon.getName().toUpperCase());
        playerHPLabel.setText("HP: " + playerCurrentHP[currentPlayerPokemon] + " / " + playerMaxHP[currentPlayerPokemon]);
        playerHealthBar.setMaximum(playerMaxHP[currentPlayerPokemon]);
        playerHealthBar.setValue(playerCurrentHP[currentPlayerPokemon]);
        updateHealthBar(true);
        updateTypeBadges(true);

        // Force repaint of the info card to prevent ghosting
        if (playerNameLabel.getParent() != null) {
            playerNameLabel.getParent().revalidate();
            playerNameLabel.getParent().repaint();
        }
    }

    private void updateEnemyInfo() {
        Pokemon pokemon = enemyTeam.get(currentEnemyPokemon);
        enemyNameLabel.setText(pokemon.getName().toUpperCase());
        enemyHPLabel.setText("HP: " + enemyCurrentHP[currentEnemyPokemon] + " / " + enemyMaxHP[currentEnemyPokemon]);
        enemyHealthBar.setMaximum(enemyMaxHP[currentEnemyPokemon]);
        enemyHealthBar.setValue(enemyCurrentHP[currentEnemyPokemon]);
        updateHealthBar(false);
        updateTypeBadges(false);

        // Force repaint of the info card to prevent ghosting
        if (enemyNameLabel.getParent() != null) {
            enemyNameLabel.getParent().revalidate();
            enemyNameLabel.getParent().repaint();
        }
    }

    private void updateTypeBadges(boolean isPlayer) {
        Pokemon pokemon = isPlayer ? playerTeam.get(currentPlayerPokemon) : enemyTeam.get(currentEnemyPokemon);
        JPanel typePanel = isPlayer ? playerTypePanel : enemyTypePanel;

        if (typePanel != null) {
            typePanel.removeAll();
            typePanel.add(createTypeBadge(pokemon.getType1()));
            if (pokemon.getType2() != null && !pokemon.getType2().isEmpty() && !pokemon.getType2().equals("None")) {
                typePanel.add(createTypeBadge(pokemon.getType2()));
            }
            typePanel.revalidate();
            typePanel.repaint();
        }
    }

    private void updateTeamSlots() {
        // Update player team slots with complete redraw
        for (int i = 0; i < 5; i++) {
            JPanel slot = playerTeamSlots[i];

            // Update background color based on HP status
            if (playerCurrentHP[i] <= 0) {
                slot.setBackground(new Color(180, 180, 180, 200));
            } else {
                slot.setBackground(new Color(255, 255, 255, 240));
            }

            Component[] components = slot.getComponents();
            for (Component c : components) {
                if (c instanceof JProgressBar hpBar) {
                    hpBar.setValue(playerCurrentHP[i]);

                    if (playerCurrentHP[i] <= 0) {
                        hpBar.setForeground(Color.GRAY);
                    } else if (playerCurrentHP[i] < playerMaxHP[i] * 0.2) {
                        hpBar.setForeground(new Color(220, 20, 60));
                    } else if (playerCurrentHP[i] < playerMaxHP[i] * 0.5) {
                        hpBar.setForeground(new Color(255, 165, 0));
                    } else {
                        hpBar.setForeground(new Color(76, 175, 80));
                    }
                    hpBar.repaint();
                }
            }

            // Complete repaint to clear any ghosting
            slot.invalidate();
            slot.revalidate();
            slot.repaint();
        }

        // Update enemy team slots with complete redraw
        for (int i = 0; i < 5; i++) {
            JPanel slot = enemyTeamSlots[i];

            // Update background color based on HP status
            if (enemyCurrentHP[i] <= 0) {
                slot.setBackground(new Color(180, 180, 180, 200));
            } else {
                slot.setBackground(new Color(255, 255, 255, 240));
            }

            Component[] components = slot.getComponents();
            for (Component c : components) {
                if (c instanceof JProgressBar hpBar) {
                    hpBar.setValue(enemyCurrentHP[i]);

                    if (enemyCurrentHP[i] <= 0) {
                        hpBar.setForeground(Color.GRAY);
                    } else if (enemyCurrentHP[i] < enemyMaxHP[i] * 0.2) {
                        hpBar.setForeground(new Color(220, 20, 60));
                    } else if (enemyCurrentHP[i] < enemyMaxHP[i] * 0.5) {
                        hpBar.setForeground(new Color(255, 165, 0));
                    } else {
                        hpBar.setForeground(new Color(76, 175, 80));
                    }
                    hpBar.repaint();
                }
            }

            // Complete repaint to clear any ghosting
            slot.invalidate();
            slot.revalidate();
            slot.repaint();
        }
    }

    private void updateHealthBar(boolean isPlayer) {
        if (isPlayer) {
            int current = playerCurrentHP[currentPlayerPokemon];
            int max = playerMaxHP[currentPlayerPokemon];
            playerHealthBar.setValue(current);
            double percentage = ((double) current / max) * 100;
            playerHealthBar.setString(String.format("%.0f%%", percentage));
            playerHPLabel.setText("HP: " + current + " / " + max);

            if (current < max * 0.2) {
                playerHealthBar.setForeground(new Color(220, 20, 60));
            } else if (current < max * 0.5) {
                playerHealthBar.setForeground(new Color(255, 165, 0));
            } else {
                playerHealthBar.setForeground(new Color(76, 175, 80));
            }
        } else {
            int current = enemyCurrentHP[currentEnemyPokemon];
            int max = enemyMaxHP[currentEnemyPokemon];
            enemyHealthBar.setValue(current);
            double percentage = ((double) current / max) * 100;
            enemyHealthBar.setString(String.format("%.0f%%", percentage));
            enemyHPLabel.setText("HP: " + current + " / " + max);

            if (current < max * 0.2) {
                enemyHealthBar.setForeground(new Color(220, 20, 60));
            } else if (current < max * 0.5) {
                enemyHealthBar.setForeground(new Color(255, 165, 0));
            } else {
                enemyHealthBar.setForeground(new Color(76, 175, 80));
            }
        }
    }

    private void animateAttack(boolean isPlayerAttacking, Runnable onComplete) {
        final int[] frame = {0};
        final int maxFrames = 15;

        JLabel attackingSprite = isPlayerAttacking ? playerSpriteLabel : enemySpriteLabel;
        JLabel defendingSprite = isPlayerAttacking ? enemySpriteLabel : playerSpriteLabel;

        final Point originalPosAtk = attackingSprite.getLocation();
        final Point originalPosDef = defendingSprite.getLocation();

        animationTimer = new javax.swing.Timer(35, null);
        animationTimer.addActionListener(e -> {
            frame[0]++;

            if (frame[0] <= 5) {
                int offset = (frame[0] % 2 == 0) ? 15 : -15;
                attackingSprite.setLocation(originalPosAtk.x + offset, originalPosAtk.y);
            } else if (frame[0] <= 10) {
                attackingSprite.setLocation(originalPosAtk);
                if (frame[0] % 2 == 0) {
                    defendingSprite.setVisible(false);
                } else {
                    defendingSprite.setVisible(true);
                }
                int offset = (frame[0] % 2 == 0) ? 12 : -12;
                defendingSprite.setLocation(originalPosDef.x + offset, originalPosDef.y);
            } else {
                defendingSprite.setVisible(true);
                defendingSprite.setLocation(originalPosDef);
            }

            if (frame[0] >= maxFrames) {
                animationTimer.stop();
                defendingSprite.setVisible(true);
                attackingSprite.setLocation(originalPosAtk);
                defendingSprite.setLocation(originalPosDef);
                SwingUtilities.invokeLater(onComplete);
            }
        });
        animationTimer.start();
    }

    private void endBattle(boolean playerWon) {
        battleEnded = true;
        disableControls();

        if (playerWon) {
            showBattleMessage("VICTORY! You defeated the Rival!", 3000, () -> {
                JOptionPane.showMessageDialog(this,
                    "CONGRATULATIONS!\n\nYou have defeated your rival!\n\nYou are the Champion!",
                    "VICTORY", JOptionPane.INFORMATION_MESSAGE);
                returnToPokedex();
            });
        } else {
            showBattleMessage("DEFEAT! All your Pokemon fainted!", 3000, () -> {
                JOptionPane.showMessageDialog(this,
                    "Oh no!\n\nAll your Pokemon have fainted!\n\nDon't give up, Trainer!",
                    "Defeat", JOptionPane.WARNING_MESSAGE);
                returnToPokedex();
            });
        }
    }

    private void enableControls() {
        for (JButton button : attackButtons) {
            button.setEnabled(true);
            // Restore original color when enabling
            Color originalColor = (Color) button.getClientProperty("originalColor");
            if (originalColor != null) {
                button.setBackground(originalColor);
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(originalColor.darker().darker(), 3, true),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
                ));
            }
        }

        Color switchColor = (Color) switchButton.getClientProperty("originalColor");
        if (switchColor != null) {
            switchButton.setBackground(switchColor);
        }
        switchButton.setEnabled(true);

        Color runColor = (Color) runButton.getClientProperty("originalColor");
        if (runColor != null) {
            runButton.setBackground(runColor);
        }
        runButton.setEnabled(true);
    }

    private void disableControls() {
        Color disabledColor = new Color(120, 120, 120);

        for (JButton button : attackButtons) {
            button.setEnabled(false);
            button.setBackground(disabledColor);
            button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 80), 3, true),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
        }

        switchButton.setEnabled(false);
        switchButton.setBackground(disabledColor);

        runButton.setEnabled(false);
        runButton.setBackground(disabledColor);
    }

    private int calculateDamage(int attack, int spAtk, int defense, int spDef, int basePower, String category, double effectiveness) {
        Random rand = new Random();
        double randomFactor = 0.85 + (rand.nextDouble() * 0.15);

        int attackStat = category != null && category.equals("special") ? spAtk : attack;
        int defenseStat = category != null && category.equals("special") ? spDef : defense;

        double damage = ((2.0 * 50 / 5 + 2) * basePower * attackStat / defenseStat) / 50 + 2;
        damage = damage * effectiveness * randomFactor;

        return Math.max(1, (int) damage);
    }

    private String getEffectivenessMessage(double effectiveness) {
        if (effectiveness >= 2.0) {
            return "\n\nIt's SUPER EFFECTIVE!";
        } else if (effectiveness <= 0.5 && effectiveness > 0) {
            return "\n\nIt's not very effective...";
        } else if (effectiveness == 0) {
            return "\n\nIt doesn't affect the enemy...";
        }
        return "";
    }

    private double getTypeEffectiveness(String attackType, String defType1, String defType2) {
        double effectiveness = getTypeMult(attackType.toLowerCase(), defType1.toLowerCase());
        if (defType2 != null && !defType2.isEmpty() && !defType2.equals("None")) {
            effectiveness *= getTypeMult(attackType.toLowerCase(), defType2.toLowerCase());
        }
        return effectiveness;
    }

    private double getTypeMult(String attackType, String defenseType) {
        Map<String, Map<String, Double>> typeChart = new HashMap<>();

        Map<String, Double> normal = new HashMap<>();
        normal.put("rock", 0.5);
        normal.put("ghost", 0.0);
        normal.put("steel", 0.5);
        typeChart.put("normal", normal);

        Map<String, Double> fire = new HashMap<>();
        fire.put("fire", 0.5);
        fire.put("water", 0.5);
        fire.put("grass", 2.0);
        fire.put("ice", 2.0);
        fire.put("bug", 2.0);
        fire.put("rock", 0.5);
        fire.put("dragon", 0.5);
        fire.put("steel", 2.0);
        typeChart.put("fire", fire);

        Map<String, Double> water = new HashMap<>();
        water.put("fire", 2.0);
        water.put("water", 0.5);
        water.put("grass", 0.5);
        water.put("ground", 2.0);
        water.put("rock", 2.0);
        water.put("dragon", 0.5);
        typeChart.put("water", water);

        Map<String, Double> electric = new HashMap<>();
        electric.put("water", 2.0);
        electric.put("electric", 0.5);
        electric.put("grass", 0.5);
        electric.put("ground", 0.0);
        electric.put("flying", 2.0);
        electric.put("dragon", 0.5);
        typeChart.put("electric", electric);

        Map<String, Double> grass = new HashMap<>();
        grass.put("fire", 0.5);
        grass.put("water", 2.0);
        grass.put("grass", 0.5);
        grass.put("poison", 0.5);
        grass.put("ground", 2.0);
        grass.put("flying", 0.5);
        grass.put("bug", 0.5);
        grass.put("rock", 2.0);
        grass.put("dragon", 0.5);
        grass.put("steel", 0.5);
        typeChart.put("grass", grass);

        Map<String, Double> ice = new HashMap<>();
        ice.put("fire", 0.5);
        ice.put("water", 0.5);
        ice.put("grass", 2.0);
        ice.put("ice", 0.5);
        ice.put("ground", 2.0);
        ice.put("flying", 2.0);
        ice.put("dragon", 2.0);
        ice.put("steel", 0.5);
        typeChart.put("ice", ice);

        Map<String, Double> fighting = new HashMap<>();
        fighting.put("normal", 2.0);
        fighting.put("ice", 2.0);
        fighting.put("poison", 0.5);
        fighting.put("flying", 0.5);
        fighting.put("psychic", 0.5);
        fighting.put("bug", 0.5);
        fighting.put("rock", 2.0);
        fighting.put("ghost", 0.0);
        fighting.put("dark", 2.0);
        fighting.put("steel", 2.0);
        fighting.put("fairy", 0.5);
        typeChart.put("fighting", fighting);

        Map<String, Double> poison = new HashMap<>();
        poison.put("grass", 2.0);
        poison.put("poison", 0.5);
        poison.put("ground", 0.5);
        poison.put("rock", 0.5);
        poison.put("ghost", 0.5);
        poison.put("steel", 0.0);
        poison.put("fairy", 2.0);
        typeChart.put("poison", poison);

        Map<String, Double> ground = new HashMap<>();
        ground.put("fire", 2.0);
        ground.put("electric", 2.0);
        ground.put("grass", 0.5);
        ground.put("poison", 2.0);
        ground.put("flying", 0.0);
        ground.put("bug", 0.5);
        ground.put("rock", 2.0);
        ground.put("steel", 2.0);
        typeChart.put("ground", ground);

        Map<String, Double> flying = new HashMap<>();
        flying.put("electric", 0.5);
        flying.put("grass", 2.0);
        flying.put("fighting", 2.0);
        flying.put("bug", 2.0);
        flying.put("rock", 0.5);
        flying.put("steel", 0.5);
        typeChart.put("flying", flying);

        Map<String, Double> psychic = new HashMap<>();
        psychic.put("fighting", 2.0);
        psychic.put("poison", 2.0);
        psychic.put("psychic", 0.5);
        psychic.put("dark", 0.0);
        psychic.put("steel", 0.5);
        typeChart.put("psychic", psychic);

        Map<String, Double> bug = new HashMap<>();
        bug.put("fire", 0.5);
        bug.put("grass", 2.0);
        bug.put("fighting", 0.5);
        bug.put("poison", 0.5);
        bug.put("flying", 0.5);
        bug.put("psychic", 2.0);
        bug.put("ghost", 0.5);
        bug.put("dark", 2.0);
        bug.put("steel", 0.5);
        bug.put("fairy", 0.5);
        typeChart.put("bug", bug);

        Map<String, Double> rock = new HashMap<>();
        rock.put("fire", 2.0);
        rock.put("ice", 2.0);
        rock.put("fighting", 0.5);
        rock.put("ground", 0.5);
        rock.put("flying", 2.0);
        rock.put("bug", 2.0);
        rock.put("steel", 0.5);
        typeChart.put("rock", rock);

        Map<String, Double> ghost = new HashMap<>();
        ghost.put("normal", 0.0);
        ghost.put("psychic", 2.0);
        ghost.put("ghost", 2.0);
        ghost.put("dark", 0.5);
        typeChart.put("ghost", ghost);

        Map<String, Double> dragon = new HashMap<>();
        dragon.put("dragon", 2.0);
        dragon.put("steel", 0.5);
        dragon.put("fairy", 0.0);
        typeChart.put("dragon", dragon);

        Map<String, Double> dark = new HashMap<>();
        dark.put("fighting", 0.5);
        dark.put("psychic", 2.0);
        dark.put("ghost", 2.0);
        dark.put("dark", 0.5);
        dark.put("fairy", 0.5);
        typeChart.put("dark", dark);

        Map<String, Double> steel = new HashMap<>();
        steel.put("fire", 0.5);
        steel.put("water", 0.5);
        steel.put("electric", 0.5);
        steel.put("ice", 2.0);
        steel.put("rock", 2.0);
        steel.put("steel", 0.5);
        steel.put("fairy", 2.0);
        typeChart.put("steel", steel);

        Map<String, Double> fairy = new HashMap<>();
        fairy.put("fire", 0.5);
        fairy.put("fighting", 2.0);
        fairy.put("poison", 0.5);
        fairy.put("dragon", 2.0);
        fairy.put("dark", 2.0);
        fairy.put("steel", 0.5);
        typeChart.put("fairy", fairy);

        if (typeChart.containsKey(attackType)) {
            return typeChart.get(attackType).getOrDefault(defenseType, 1.0);
        }
        return 1.0;
    }

    private Attack[] generateAttacksFromJSON(Pokemon pokemon) {
        Attack[] attacks = new Attack[4];
        String pokemonName = pokemon.getName();

        try {
            if (movesPokemon.has(pokemonName)) {
                JSONArray moves = movesPokemon.getJSONArray(pokemonName);

                for (int i = 0; i < Math.min(4, moves.length()); i++) {
                    String moveName = moves.getString(i);

                    if (movesData.has(moveName)) {
                        JSONObject moveData = movesData.getJSONObject(moveName);

                        int power = moveData.optInt("power", 50);
                        String type = moveData.optString("type", "normal");
                        String category = moveData.optString("category", "physical");

                        attacks[i] = new Attack(moveName, type, power, category);
                    } else {
                        attacks[i] = new Attack(moveName, "normal", 50, "physical");
                    }
                }

                for (int i = moves.length(); i < 4; i++) {
                    attacks[i] = new Attack("Tackle", "normal", 40, "physical");
                }

            } else {
                attacks = generateFallbackAttacks(pokemon);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading moves for " + pokemonName, e);
            attacks = generateFallbackAttacks(pokemon);
        }

        return attacks;
    }

    private Attack[] generateFallbackAttacks(Pokemon pokemon) {
        String type1 = pokemon.getType1();
        Attack[] attacks = new Attack[4];
        attacks[0] = new Attack("Tackle", "normal", 40, "physical");
        attacks[1] = new Attack(type1 + " Attack", type1.toLowerCase(), 60, "physical");
        attacks[2] = new Attack(type1 + " Beam", type1.toLowerCase(), 80, "special");
        attacks[3] = new Attack("Hyper Beam", "normal", 150, "special");
        return attacks;
    }

    private JLabel createTypeBadge(String type) {
        JLabel badge = new JLabel(" " + type.toUpperCase() + " ");
        badge.setFont(new Font("Arial", Font.BOLD, 10));
        badge.setForeground(Color.WHITE);
        badge.setOpaque(true);
        badge.setBackground(getTypeColor(type));
        badge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        return badge;
    }

    private Color getTypeColor(String type) {
        return switch (type.toLowerCase()) {
            case "fire" -> new Color(240, 128, 48);
            case "water" -> new Color(104, 144, 240);
            case "electric" -> new Color(248, 208, 48);
            case "grass" -> new Color(120, 200, 80);
            case "ice" -> new Color(152, 216, 216);
            case "fighting" -> new Color(192, 48, 40);
            case "poison" -> new Color(160, 64, 160);
            case "ground" -> new Color(224, 192, 104);
            case "flying" -> new Color(168, 144, 240);
            case "psychic" -> new Color(248, 88, 136);
            case "bug" -> new Color(168, 184, 32);
            case "rock" -> new Color(184, 160, 56);
            case "ghost" -> new Color(112, 88, 152);
            case "dragon" -> new Color(112, 56, 248);
            case "dark" -> new Color(112, 88, 72);
            case "steel" -> new Color(184, 184, 208);
            case "fairy" -> new Color(238, 153, 172);
            default -> new Color(168, 168, 120);
        };
    }

    private ImageIcon loadPokemonSprite(int id, boolean isEnemy) {
        String dir = isEnemy ? FRONT_IMAGE_DIR : BACK_IMAGE_DIR;
        String file = dir + id + ".gif";
        File f = new File(file);

        if (!f.exists()) {
            LOGGER.log(Level.WARNING, "Sprite not found: " + file);
            return createPlaceholderSprite();
        }

        try {
            // Load GIF from URL - preserves animation WITHOUT scaling
            // Scaling animated GIFs causes palette/color corruption
            java.net.URL url = f.toURI().toURL();
            ImageIcon originalIcon = new ImageIcon(url);

            // Get original dimensions
            int originalWidth = originalIcon.getIconWidth();
            int originalHeight = originalIcon.getIconHeight();

            if (originalWidth <= 0 || originalHeight <= 0) {
                LOGGER.log(Level.WARNING, "Invalid sprite dimensions: " + file);
                return createPlaceholderSprite();
            }

            // Set target size to 200px (smaller and cleaner than 400px)
            int targetSize = 200;

            // Calculate scaling while maintaining aspect ratio
            double scale = (double) targetSize / Math.max(originalWidth, originalHeight);
            int scaledWidth = (int) (originalWidth * scale);
            int scaledHeight = (int) (originalHeight * scale);

            // For GIFs, use SCALE_DEFAULT to prevent color corruption
            // SCALE_DEFAULT doesn't interfere with GIF color palette
            Image scaledImage = originalIcon.getImage().getScaledInstance(
                scaledWidth, scaledHeight, Image.SCALE_DEFAULT
            );

            return new ImageIcon(scaledImage);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading sprite: " + file, e);
            return createPlaceholderSprite();
        }
    }

    private ImageIcon loadPokemonSmallIcon(int id) {
        String file = ICON_IMAGE_DIR + id + ".png";
        File f = new File(file);

        if (!f.exists()) {
            return createPlaceholderIcon();
        }

        ImageIcon icon = new ImageIcon(file);
        Image img = icon.getImage().getScaledInstance(55, 55, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    private ImageIcon createPlaceholderSprite() {
        BufferedImage placeholder = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = placeholder.createGraphics();
        g2d.setColor(new Color(100, 100, 100));
        g2d.fillOval(40, 40, 120, 120);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("No Sprite", 60, 105);
        g2d.dispose();
        return new ImageIcon(placeholder);
    }

    private ImageIcon createPlaceholderIcon() {
        BufferedImage placeholder = new BufferedImage(55, 55, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = placeholder.createGraphics();
        g2d.setColor(Color.GRAY);
        g2d.fillOval(8, 8, 40, 40);
        g2d.dispose();
        return new ImageIcon(placeholder);
    }

    private void returnToPokedex() {
        parentFrame.getContentPane().removeAll();
        parentFrame.setContentPane(new PokedexPanel(pokedexConn, usuariosConn, parentFrame, username));
        parentFrame.revalidate();
        parentFrame.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Epic battle background gradient - smoother and more vibrant
        GradientPaint gp = new GradientPaint(0, 0, new Color(45, 52, 90),
                                             0, getHeight(), new Color(88, 103, 168));
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Battle arena ground with better positioning and opacity
        int groundY = getHeight() / 2 + 80;

        // Ground shadow for depth
        g2d.setColor(new Color(50, 80, 40, 80));
        g2d.fillRect(0, groundY - 5, getWidth(), 5);

        // Main ground
        GradientPaint groundGradient = new GradientPaint(
            0, groundY, new Color(106, 168, 79, 120),
            0, getHeight(), new Color(78, 124, 58, 140));
        g2d.setPaint(groundGradient);
        g2d.fillRect(0, groundY, getWidth(), getHeight() - groundY);

        // Enhanced grid lines for depth effect with perspective
        g2d.setColor(new Color(78, 124, 58, 80));
        for (int i = 0; i < 8; i++) {
            int y = groundY + (i * 40);
            if (y < getHeight()) {
                // Thicker lines closer to camera (bottom)
                int lineThickness = 1 + (i / 3);
                for (int t = 0; t < lineThickness; t++) {
                    g2d.drawLine(0, y + t, getWidth(), y + t);
                }
            }
        }

        // Vertical perspective lines for more depth
        g2d.setColor(new Color(78, 124, 58, 40));
        int numVerticalLines = 12;
        for (int i = 1; i < numVerticalLines; i++) {
            int x = (getWidth() * i) / numVerticalLines;
            g2d.drawLine(x, groundY, x, getHeight());
        }
    }

    private static class Attack {
        String name;
        String type;
        int power;
        String category;

        Attack(String name, String type, int power, String category) {
            this.name = name;
            this.type = type;
            this.power = power;
            this.category = category;
        }
    }
}
