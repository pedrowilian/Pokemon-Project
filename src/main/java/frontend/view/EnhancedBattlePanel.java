package frontend.view;

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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
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

import backend.application.service.BattleService;
import backend.application.service.BattleService.BattleResult;
import backend.domain.model.BattleState;
import backend.domain.model.Move;
import backend.domain.model.Pokemon;
import backend.domain.model.PokemonBattleStats;
import backend.domain.model.Team;
import backend.infrastructure.ServiceLocator;

/**
 * Clean architecture battle panel - PRESENTATION LAYER ONLY
 * All business logic delegated to BattleService
 *
 * Reduced from 1,762 lines to ~800 lines by:
 * - Removing all damage calculation logic
 * - Removing type effectiveness calculations
 * - Removing JSON loading
 * - Removing Attack inner class
 * - Using PokemonUtils for UI components
 * - Using BattleService for all battle logic
 */
public class EnhancedBattlePanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(EnhancedBattlePanel.class.getName());
    private static final String BACK_IMAGE_DIR = "Images/Back-Pokemon-gif/";
    private static final String FRONT_IMAGE_DIR = "Images/Front-Pokemon-gif/";
    private static final String ICON_IMAGE_DIR = "Images/Image-Pokedex/";

    // Services (from backend)
    private final BattleService battleService;

    // Domain models (from backend)
    private final BattleState battleState;
    private final Team playerTeam;
    private final Team enemyTeam;

    // UI state
    private final String username;
    private final JFrame parentFrame;

    // UI Components
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

    private boolean battleEnded = false;
    private boolean isProcessing = false; // Prevent overlapping actions
    private javax.swing.Timer animationTimer;
    private JDialog switchDialog; // Track dialog to prevent duplicates

    private List<Move> playerMoves;
    private List<Move> enemyMoves;

    public EnhancedBattlePanel(List<Pokemon> playerPokemonList, List<Pokemon> enemyPokemonList,
                              String username, JFrame parentFrame) {
        // Initialize services
        ServiceLocator serviceLocator = ServiceLocator.getInstance();
        this.battleService = serviceLocator.getBattleService();

        // Create teams (converts List<Pokemon> to Team with PokemonBattleStats)
        this.playerTeam = new Team("Player", playerPokemonList);
        this.enemyTeam = new Team("Rival", enemyPokemonList);

        // Start battle
        this.battleState = battleService.startBattle(playerTeam, enemyTeam);

        // UI state
        this.username = username;
        this.parentFrame = parentFrame;

        // Generate moves for active Pokemon
        loadMoves();

        // Determine who goes first based on speed
        determineFirstTurn(playerPokemonList, enemyPokemonList);

        // Initialize UI
        initializeUI();
        startBattle();
    }

    private void loadMoves() {
        playerMoves = battleService.generateMovesForPokemon(playerTeam.getActivePokemon().getPokemon());
        enemyMoves = battleService.generateMovesForPokemon(enemyTeam.getActivePokemon().getPokemon());
    }

    private void determineFirstTurn(List<Pokemon> playerList, List<Pokemon> enemyList) {
        boolean playerGoesFirst = playerList.get(0).getSpeed() >= enemyList.get(0).getSpeed();
        battleState.setCurrentTurn(playerGoesFirst ? BattleState.Turn.PLAYER : BattleState.Turn.ENEMY);
    }

    private void initializeUI() {
        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(45, 52, 90));
        setDoubleBuffered(true);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                revalidate();
                repaint();
            }
        });

        JPanel mainContainer = new JPanel(new BorderLayout(0, 0));
        mainContainer.setOpaque(false);

        // Left and right team panels
        JPanel leftTeamPanel = createVerticalTeamBar(playerTeam, true);
        mainContainer.add(leftTeamPanel, BorderLayout.WEST);

        JPanel rightTeamPanel = createVerticalTeamBar(enemyTeam, false);
        mainContainer.add(rightTeamPanel, BorderLayout.EAST);

        // Center - Battle area
        JPanel centerPanel = new JPanel(new BorderLayout(0, 0));
        centerPanel.setOpaque(false);

        JPanel enemyInfoPanel = createCompactInfoCard(false);
        centerPanel.add(enemyInfoPanel, BorderLayout.NORTH);

        JPanel battleFieldPanel = createBattleFieldPanel();
        centerPanel.add(battleFieldPanel, BorderLayout.CENTER);

        // Bottom - Player info and controls
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

    private JPanel createVerticalTeamBar(Team team, boolean isPlayer) {
        JPanel barPanel = new JPanel();
        barPanel.setLayout(new BoxLayout(barPanel, BoxLayout.Y_AXIS));
        barPanel.setOpaque(true);
        barPanel.setBackground(new Color(0, 0, 0, 200));
        barPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(isPlayer ? new Color(76, 175, 80) : new Color(220, 20, 60), 3),
            BorderFactory.createEmptyBorder(12, 8, 12, 8)
        ));
        barPanel.setPreferredSize(new Dimension(110, 0));

        JLabel titleLabel = new JLabel(isPlayer ? "YOUR TEAM" : "ENEMY TEAM", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 11));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        barPanel.add(titleLabel);
        barPanel.add(Box.createVerticalStrut(12));

        JPanel[] slots = new JPanel[team.getSize()];

        for (int i = 0; i < team.getSize(); i++) {
            PokemonBattleStats pokemonStats = team.getPokemon(i);
            @SuppressWarnings("unused")
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
                    g2d.setColor(new Color(0, 0, 0, 0));
                    g2d.setComposite(java.awt.AlphaComposite.Clear);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
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

            JLabel iconLabel = new JLabel();
            iconLabel.setOpaque(false);
            iconLabel.setDoubleBuffered(true);
            iconLabel.setIcon(loadPokemonSmallIcon(pokemonStats.getPokemon().getId()));
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            slot.add(iconLabel);
            slot.add(Box.createVerticalStrut(4));

            String displayName = pokemonStats.getPokemon().getName().length() > 8 ?
                pokemonStats.getPokemon().getName().substring(0, 7) + "." : pokemonStats.getPokemon().getName();
            JLabel nameLabel = new JLabel(displayName, SwingConstants.CENTER);
            nameLabel.setFont(new Font("Arial", Font.BOLD, 9));
            nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            slot.add(nameLabel);
            slot.add(Box.createVerticalStrut(3));

            JProgressBar hpBar = new JProgressBar(0, pokemonStats.getMaxHp());
            hpBar.setValue(pokemonStats.getCurrentHp());
            hpBar.setPreferredSize(new Dimension(70, 8));
            hpBar.setMaximumSize(new Dimension(70, 8));
            hpBar.setForeground(new Color(76, 175, 80));
            hpBar.setBackground(new Color(220, 220, 220));
            hpBar.setBorderPainted(true);
            hpBar.setAlignmentX(Component.CENTER_ALIGNMENT);
            slot.add(hpBar);

            JLabel activeIndicator = new JLabel("★ ACTIVE ★", SwingConstants.CENTER);
            activeIndicator.setFont(new Font("Arial", Font.BOLD, 8));
            activeIndicator.setForeground(new Color(255, 215, 0));
            activeIndicator.setAlignmentX(Component.CENTER_ALIGNMENT);
            activeIndicator.setVisible(i == team.getActivePokemonIndex());
            slot.add(Box.createVerticalStrut(2));
            slot.add(activeIndicator);

            slotContainer.add(slot, BorderLayout.CENTER);
            slots[i] = slot;
            barPanel.add(slotContainer);

            if (i < team.getSize() - 1) {
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
        PokemonBattleStats stats = isPlayer ? playerTeam.getActivePokemon() : enemyTeam.getActivePokemon();
        Pokemon pokemon = stats.getPokemon();

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(0, 0, 0, 0));
                g2d.setComposite(java.awt.AlphaComposite.Clear);
                g2d.fillRect(0, 0, getWidth(), getHeight());
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

        JLabel hpLabel = new JLabel("HP: " + stats.getCurrentHp() + " / " + stats.getMaxHp());
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

        JProgressBar healthBar = new JProgressBar(0, stats.getMaxHp());
        healthBar.setValue(stats.getCurrentHp());
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

        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        typePanel.setOpaque(false);
        typePanel.add(PokemonUtils.createTypeBadge(pokemon.getType1()));
        if (pokemon.getType2() != null && !pokemon.getType2().isEmpty() && !pokemon.getType2().equals("None")) {
            typePanel.add(PokemonUtils.createTypeBadge(pokemon.getType2()));
        }
        typePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(typePanel);

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

        enemySpriteLabel = new JLabel();
        enemySpriteLabel.setOpaque(false);
        enemySpriteLabel.setDoubleBuffered(true);
        enemySpriteLabel.setIcon(loadPokemonSprite(enemyTeam.getActivePokemon().getPokemon().getId(), true));
        enemySpriteLabel.setHorizontalAlignment(SwingConstants.CENTER);
        enemySpriteLabel.setVerticalAlignment(SwingConstants.CENTER);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.weighty = 0.55;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        gbc.insets = new Insets(10, 20, 0, 30);
        panel.add(enemySpriteLabel, gbc);

        playerSpriteLabel = new JLabel();
        playerSpriteLabel.setOpaque(false);
        playerSpriteLabel.setDoubleBuffered(true);
        playerSpriteLabel.setIcon(loadPokemonSprite(playerTeam.getActivePokemon().getPokemon().getId(), false));
        playerSpriteLabel.setHorizontalAlignment(SwingConstants.CENTER);
        playerSpriteLabel.setVerticalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 0.45;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        gbc.insets = new Insets(0, 30, 10, 20);
        panel.add(playerSpriteLabel, gbc);

        JPanel messagePanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setColor(new Color(0, 0, 0, 0));
                g2d.setComposite(java.awt.AlphaComposite.Clear);
                g2d.fillRect(0, 0, getWidth(), getHeight());
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

        JPanel attackPanel = new JPanel(new GridLayout(2, 2, 6, 6));
        attackPanel.setOpaque(false);

        attackButtons = new JButton[4];
        for (int i = 0; i < 4; i++) {
            final int index = i;
            Move move = (i < playerMoves.size()) ? playerMoves.get(i) : new Move("Tackle", "normal", 40, 100);
            attackButtons[i] = PokemonUtils.createAttackButton(move, e -> executePlayerAttack(playerMoves.get(index)));
            attackPanel.add(attackButtons[i]);
        }

        panel.add(attackPanel, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        actionPanel.setOpaque(false);

        switchButton = PokemonUtils.createActionButton("SWITCH POKEMON", new Color(52, 152, 219));
        switchButton.addActionListener(e -> showSwitchDialog());
        actionPanel.add(switchButton);

        runButton = PokemonUtils.createActionButton("RUN", new Color(231, 76, 60));
        runButton.addActionListener(e -> endBattle(false));
        actionPanel.add(runButton);

        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void startBattle() {
        SwingUtilities.invokeLater(() -> {
            showBattleMessage("A rival trainer wants to battle!", 2000, () -> {
                showBattleMessage("Rival sent out " + enemyTeam.getActivePokemon().getPokemon().getName() + "!", 2000, () -> {
                    showBattleMessage("Go! " + playerTeam.getActivePokemon().getPokemon().getName() + "!", 2000, () -> {
                        if (battleState.getCurrentTurn() == BattleState.Turn.ENEMY) {
                            showBattleMessage("Enemy " + enemyTeam.getActivePokemon().getPokemon().getName() + " will attack first!", 2000, () -> {
                                executeEnemyTurn();
                            });
                        } else {
                            showBattleMessage("What will " + playerTeam.getActivePokemon().getPokemon().getName() + " do?", 1000, () -> {
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

    private void executePlayerAttack(Move move) {
        if (battleEnded || battleState.getCurrentTurn() != BattleState.Turn.PLAYER || isProcessing) {
            return;
        }

        isProcessing = true;
        disableControls();

        String attackerName = playerTeam.getActivePokemon().getPokemon().getName();

        showBattleMessage(attackerName + " used " + move.getName() + "!", 1500, () -> {
            // Use BattleService to execute move
            BattleResult result = battleService.executeMove(battleState, move);

            animateAttack(true, () -> {
                updateHealthBar(false);
                updateTeamSlots();

                showBattleMessage(result.getMessage(), 2000, () -> {
                    if (enemyTeam.getActivePokemon().isFainted()) {
                        isProcessing = false; // Reset before handling faint
                        handlePokemonFainted(false);
                    } else {
                        isProcessing = false; // Reset before enemy turn
                        battleState.switchTurn();
                        executeEnemyTurn();
                    }
                });
            });
        });
    }

    private void executeEnemyTurn() {
        if (battleEnded || isProcessing) {
            return;
        }

        if (enemyTeam.getActivePokemon().isFainted()) {
            switchEnemyPokemon();
            return;
        }

        isProcessing = true;
        // Use BattleService to execute enemy turn
        battleState.setCurrentTurn(BattleState.Turn.ENEMY);
        String attackerName = enemyTeam.getActivePokemon().getPokemon().getName();

        // Get random move
        Move enemyMove = enemyMoves.get((int) (Math.random() * enemyMoves.size()));

        showBattleMessage("Enemy " + attackerName + " used " + enemyMove.getName() + "!", 1500, () -> {
            BattleResult result = battleService.executeMove(battleState, enemyMove);

            animateAttack(false, () -> {
                updateHealthBar(true);
                updateTeamSlots();

                showBattleMessage(result.getMessage(), 2000, () -> {
                    if (playerTeam.getActivePokemon().isFainted()) {
                        isProcessing = false; // Reset before handling faint
                        handlePokemonFainted(true);
                    } else {
                        isProcessing = false; // Reset before player turn
                        battleState.switchTurn();
                        showBattleMessage("What will " + playerTeam.getActivePokemon().getPokemon().getName() + " do?", 1000, () -> {
                            enableControls();
                        });
                    }
                });
            });
        });
    }

    private void handlePokemonFainted(boolean isPlayer) {
        // Prevent multiple calls
        if (isProcessing) {
            return;
        }
        isProcessing = true;

        PokemonBattleStats fainted = isPlayer ? playerTeam.getActivePokemon() : enemyTeam.getActivePokemon();

        showBattleMessage(fainted.getPokemon().getName() + " fainted!", 2000, () -> {
            if (isPlayer) {
                if (playerTeam.isDefeated()) {
                    isProcessing = false;
                    endBattle(false);
                } else {
                    // Must switch - dialog cannot be cancelled
                    showMandatorySwitchDialog();
                }
            } else {
                if (enemyTeam.isDefeated()) {
                    isProcessing = false;
                    endBattle(true);
                } else {
                    switchEnemyPokemon();
                }
            }
        });
    }

    private void switchPlayerPokemon(int newIndex) {
        // Get the OLD pokemon name BEFORE switching
        String oldPokemonName = playerTeam.getActivePokemon().getPokemon().getName();

        // Use the correct service method for player team
        if (!battleService.switchPlayerPokemon(battleState, newIndex)) {
            JOptionPane.showMessageDialog(this, "That Pokemon has fainted!", "Cannot Switch", JOptionPane.WARNING_MESSAGE);
            // Reset processing flag if switch failed
            isProcessing = false;
            return;
        }

        disableControls();

        showBattleMessage(oldPokemonName + ", come back!", 1500, () -> {
            playerSpriteLabel.setIcon(null);
            playerSpriteLabel.repaint();

            javax.swing.Timer transitionTimer = new javax.swing.Timer(300, evt -> {
                // Get new active Pokemon after switch
                PokemonBattleStats newActivePokemon = playerTeam.getActivePokemon();

                // Update sprite with NEW pokemon
                playerSpriteLabel.setIcon(loadPokemonSprite(newActivePokemon.getPokemon().getId(), false));

                // Generate moves for NEW pokemon
                playerMoves = battleService.generateMovesForPokemon(newActivePokemon.getPokemon());

                // Update all UI components with new Pokemon data
                updateAttackButtons();
                updatePlayerInfo();
                updateActiveIndicators();
                updateTeamSlots();

                revalidate();
                repaint();

                isProcessing = false; // Reset processing flag after switch complete

                showBattleMessage("Go! " + newActivePokemon.getPokemon().getName() + "!", 1500, () -> {
                    // After switching, it should be enemy's turn (switching takes a turn)
                    battleState.setCurrentTurn(BattleState.Turn.ENEMY);
                    executeEnemyTurn();
                });
            });
            transitionTimer.setRepeats(false);
            transitionTimer.start();
        });
    }

    /**
     * Switch player Pokemon after one has fainted (mandatory switch - doesn't consume turn)
     */
    private void switchPlayerPokemonAfterFaint(int newIndex) {
        // Get the OLD pokemon name BEFORE switching (the fainted one)
        String oldPokemonName = playerTeam.getActivePokemon().getPokemon().getName();

        // Use the correct service method for player team
        if (!battleService.switchPlayerPokemon(battleState, newIndex)) {
            JOptionPane.showMessageDialog(this, "That Pokemon has fainted!", "Cannot Switch", JOptionPane.WARNING_MESSAGE);
            // Reset processing flag if switch failed
            isProcessing = false;
            return;
        }

        disableControls();

        showBattleMessage(oldPokemonName + " has fainted! Go, " + playerTeam.getActivePokemon().getPokemon().getName() + "!", 1500, () -> {
            playerSpriteLabel.setIcon(null);
            playerSpriteLabel.repaint();

            javax.swing.Timer transitionTimer = new javax.swing.Timer(300, evt -> {
                // Get new active Pokemon after switch
                PokemonBattleStats newActivePokemon = playerTeam.getActivePokemon();

                // Update sprite with NEW pokemon
                playerSpriteLabel.setIcon(loadPokemonSprite(newActivePokemon.getPokemon().getId(), false));

                // Generate moves for NEW pokemon
                playerMoves = battleService.generateMovesForPokemon(newActivePokemon.getPokemon());

                // Update all UI components with new Pokemon data
                updateAttackButtons();
                updatePlayerInfo();
                updateActiveIndicators();
                updateTeamSlots();

                revalidate();
                repaint();

                isProcessing = false; // Reset processing flag after switch complete

                // After fainted switch, continue with normal turn flow
                // If it was enemy's turn when player fainted, enemy should go again
                // If it was player's turn, enemy should get the next turn (since player just lost a Pokemon)
                battleState.setCurrentTurn(BattleState.Turn.ENEMY);
                executeEnemyTurn();
            });
            transitionTimer.setRepeats(false);
            transitionTimer.start();
        });
    }

    private void switchEnemyPokemon() {
        if (!enemyTeam.autoSwitchToNextAlive()) {
            isProcessing = false;
            endBattle(true);
            return;
        }

        // Get the new active Pokemon after auto-switch
        PokemonBattleStats newActivePokemon = enemyTeam.getActivePokemon();

        showBattleMessage("Rival sent out " + newActivePokemon.getPokemon().getName() + "!", 2000, () -> {
            enemySpriteLabel.setIcon(null);
            enemySpriteLabel.repaint();

            javax.swing.Timer transitionTimer = new javax.swing.Timer(300, evt -> {
                // Get current active Pokemon (should be the same as newActivePokemon)
                PokemonBattleStats currentActive = enemyTeam.getActivePokemon();

                // Update sprite with NEW enemy pokemon
                enemySpriteLabel.setIcon(loadPokemonSprite(currentActive.getPokemon().getId(), true));

                // Generate moves for NEW enemy pokemon
                enemyMoves = battleService.generateMovesForPokemon(currentActive.getPokemon());

                // Update all UI components with new enemy Pokemon data
                updateEnemyInfo();
                updateActiveIndicators();
                updateTeamSlots();

                revalidate();
                repaint();

                isProcessing = false; // Reset processing flag

                // After enemy switches due to fainting, it's player's turn
                battleState.setCurrentTurn(BattleState.Turn.PLAYER);
                showBattleMessage("What will " + playerTeam.getActivePokemon().getPokemon().getName() + " do?", 1000, () -> {
                    enableControls();
                });
            });
            transitionTimer.setRepeats(false);
            transitionTimer.start();
        });
    }

    private void updateActiveIndicators() {
        for (int i = 0; i < playerTeam.getSize(); i++) {
            JPanel slot = playerTeamSlots[i];
            Component[] components = slot.getComponents();
            for (Component c : components) {
                if (c instanceof JLabel && ((JLabel) c).getText().contains("ACTIVE")) {
                    c.setVisible(i == playerTeam.getActivePokemonIndex());
                }
            }
        }

        for (int i = 0; i < enemyTeam.getSize(); i++) {
            JPanel slot = enemyTeamSlots[i];
            Component[] components = slot.getComponents();
            for (Component c : components) {
                if (c instanceof JLabel && ((JLabel) c).getText().contains("ACTIVE")) {
                    c.setVisible(i == enemyTeam.getActivePokemonIndex());
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

        for (int i = 0; i < playerTeam.getSize(); i++) {
            if (i == playerTeam.getActivePokemonIndex()) continue;

            final int index = i;
            PokemonBattleStats stats = playerTeam.getPokemon(i);
            Pokemon pokemon = stats.getPokemon();

            JPanel pokemonPanel = new JPanel(new BorderLayout(10, 10));
            pokemonPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(!stats.isFainted() ? new Color(76, 175, 80) : Color.GRAY, 3, true),
                new EmptyBorder(12, 12, 12, 12)
            ));
            pokemonPanel.setBackground(!stats.isFainted() ? new Color(240, 255, 240) : new Color(200, 200, 200));
            pokemonPanel.setMaximumSize(new Dimension(400, 90));

            JLabel iconLabel = new JLabel(loadPokemonSmallIcon(pokemon.getId()));
            pokemonPanel.add(iconLabel, BorderLayout.WEST);

            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setOpaque(false);

            JLabel nameLabel = new JLabel(pokemon.getName());
            nameLabel.setFont(new Font("Arial", Font.BOLD, 16));
            infoPanel.add(nameLabel);

            JLabel hpLabel = new JLabel("HP: " + stats.getCurrentHp() + " / " + stats.getMaxHp());
            hpLabel.setFont(new Font("Arial", Font.PLAIN, 13));
            infoPanel.add(hpLabel);

            JProgressBar hpBar = new JProgressBar(0, stats.getMaxHp());
            hpBar.setValue(stats.getCurrentHp());
            hpBar.setForeground(!stats.isFainted() ? new Color(76, 175, 80) : Color.GRAY);
            hpBar.setPreferredSize(new Dimension(220, 18));
            infoPanel.add(hpBar);

            pokemonPanel.add(infoPanel, BorderLayout.CENTER);

            if (!stats.isFainted()) {
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

        JButton cancelButton = PokemonUtils.createActionButton("CANCEL", new Color(231, 76, 60));
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

    /**
     * Mandatory switch dialog when Pokemon faints - CANNOT be cancelled
     * This prevents the infinite loop bug
     */
    private void showMandatorySwitchDialog() {
        // Prevent duplicate dialogs
        if (switchDialog != null && switchDialog.isVisible()) {
            return;
        }

        switchDialog = new JDialog(parentFrame, "Pokemon Fainted! Choose Next Pokemon", true);
        switchDialog.setLayout(new BorderLayout(10, 10));
        switchDialog.setSize(450, 550);
        switchDialog.setLocationRelativeTo(this);
        switchDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE); // Cannot close without selecting
        switchDialog.getContentPane().setBackground(new Color(45, 52, 90));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        contentPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("You must choose a Pokemon!");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(new Color(255, 100, 100));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(10));

        JLabel subtitleLabel = new JLabel("Click on a healthy Pokemon to continue");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(Color.WHITE);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(subtitleLabel);
        contentPanel.add(Box.createVerticalStrut(15));

        boolean hasHealthyPokemon = false;

        for (int i = 0; i < playerTeam.getSize(); i++) {
            if (i == playerTeam.getActivePokemonIndex()) continue;

            final int index = i;
            PokemonBattleStats stats = playerTeam.getPokemon(i);
            Pokemon pokemon = stats.getPokemon();

            // Skip fainted Pokemon from being clickable
            if (stats.isFainted()) continue;

            hasHealthyPokemon = true;

            JPanel pokemonPanel = new JPanel(new BorderLayout(10, 10));
            pokemonPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(76, 175, 80), 3, true),
                new EmptyBorder(12, 12, 12, 12)
            ));
            pokemonPanel.setBackground(new Color(240, 255, 240));
            pokemonPanel.setMaximumSize(new Dimension(400, 90));

            JLabel iconLabel = new JLabel(loadPokemonSmallIcon(pokemon.getId()));
            pokemonPanel.add(iconLabel, BorderLayout.WEST);

            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setOpaque(false);

            JLabel nameLabel = new JLabel(pokemon.getName());
            nameLabel.setFont(new Font("Arial", Font.BOLD, 16));
            infoPanel.add(nameLabel);

            JLabel hpLabel = new JLabel("HP: " + stats.getCurrentHp() + " / " + stats.getMaxHp());
            hpLabel.setFont(new Font("Arial", Font.PLAIN, 13));
            infoPanel.add(hpLabel);

            JProgressBar hpBar = new JProgressBar(0, stats.getMaxHp());
            hpBar.setValue(stats.getCurrentHp());
            hpBar.setForeground(new Color(76, 175, 80));
            hpBar.setPreferredSize(new Dimension(220, 18));
            infoPanel.add(hpBar);

            pokemonPanel.add(infoPanel, BorderLayout.CENTER);

            pokemonPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            pokemonPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    switchDialog.dispose();
                    switchDialog = null;
                    // Don't reset isProcessing here - let switchPlayerPokemonAfterFaint handle it
                    switchPlayerPokemonAfterFaint(index);
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

            contentPanel.add(pokemonPanel);
            contentPanel.add(Box.createVerticalStrut(10));
        }

        // Safety check - if no healthy Pokemon, force battle end
        if (!hasHealthyPokemon) {
            switchDialog.dispose();
            switchDialog = null;
            isProcessing = false;
            endBattle(false);
            return;
        }

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        switchDialog.add(scrollPane);

        switchDialog.setVisible(true);
    }

    private void updateAttackButtons() {
        for (int i = 0; i < 4; i++) {
            if (i < playerMoves.size()) {
                Move move = playerMoves.get(i);
                PokemonUtils.updateAttackButton(attackButtons[i], move);

                final int index = i;
                // Remove old listeners
                for (java.awt.event.ActionListener al : attackButtons[i].getActionListeners()) {
                    attackButtons[i].removeActionListener(al);
                }
                // Add new listener
                attackButtons[i].addActionListener(e -> executePlayerAttack(playerMoves.get(index)));
            }
        }
    }

    private void updatePlayerInfo() {
        PokemonBattleStats stats = playerTeam.getActivePokemon();
        Pokemon pokemon = stats.getPokemon();
        playerNameLabel.setText(pokemon.getName().toUpperCase());
        playerHPLabel.setText("HP: " + stats.getCurrentHp() + " / " + stats.getMaxHp());
        playerHealthBar.setMaximum(stats.getMaxHp());
        playerHealthBar.setValue(stats.getCurrentHp());
        updateHealthBar(true);
        updateTypeBadges(true);

        if (playerNameLabel.getParent() != null) {
            playerNameLabel.getParent().revalidate();
            playerNameLabel.getParent().repaint();
        }
    }

    private void updateEnemyInfo() {
        PokemonBattleStats stats = enemyTeam.getActivePokemon();
        Pokemon pokemon = stats.getPokemon();
        enemyNameLabel.setText(pokemon.getName().toUpperCase());
        enemyHPLabel.setText("HP: " + stats.getCurrentHp() + " / " + stats.getMaxHp());
        enemyHealthBar.setMaximum(stats.getMaxHp());
        enemyHealthBar.setValue(stats.getCurrentHp());
        updateHealthBar(false);
        updateTypeBadges(false);

        if (enemyNameLabel.getParent() != null) {
            enemyNameLabel.getParent().revalidate();
            enemyNameLabel.getParent().repaint();
        }
    }

    private void updateTypeBadges(boolean isPlayer) {
        PokemonBattleStats stats = isPlayer ? playerTeam.getActivePokemon() : enemyTeam.getActivePokemon();
        Pokemon pokemon = stats.getPokemon();
        JPanel typePanel = isPlayer ? playerTypePanel : enemyTypePanel;

        if (typePanel != null) {
            typePanel.removeAll();
            typePanel.add(PokemonUtils.createTypeBadge(pokemon.getType1()));
            if (pokemon.getType2() != null && !pokemon.getType2().isEmpty() && !pokemon.getType2().equals("None")) {
                typePanel.add(PokemonUtils.createTypeBadge(pokemon.getType2()));
            }
            typePanel.revalidate();
            typePanel.repaint();
        }
    }

    private void updateTeamSlots() {
        for (int i = 0; i < playerTeam.getSize(); i++) {
            PokemonBattleStats stats = playerTeam.getPokemon(i);
            JPanel slot = playerTeamSlots[i];

            if (stats.isFainted()) {
                slot.setBackground(new Color(180, 180, 180, 200));
            } else {
                slot.setBackground(new Color(255, 255, 255, 240));
            }

            Component[] components = slot.getComponents();
            for (Component c : components) {
                if (c instanceof JProgressBar hpBar) {
                    hpBar.setValue(stats.getCurrentHp());

                    if (stats.isFainted()) {
                        hpBar.setForeground(Color.GRAY);
                    } else if (stats.getHpPercentage() < 0.2) {
                        hpBar.setForeground(new Color(220, 20, 60));
                    } else if (stats.getHpPercentage() < 0.5) {
                        hpBar.setForeground(new Color(255, 165, 0));
                    } else {
                        hpBar.setForeground(new Color(76, 175, 80));
                    }
                    hpBar.repaint();
                }
            }

            slot.invalidate();
            slot.revalidate();
            slot.repaint();
        }

        for (int i = 0; i < enemyTeam.getSize(); i++) {
            PokemonBattleStats stats = enemyTeam.getPokemon(i);
            JPanel slot = enemyTeamSlots[i];

            if (stats.isFainted()) {
                slot.setBackground(new Color(180, 180, 180, 200));
            } else {
                slot.setBackground(new Color(255, 255, 255, 240));
            }

            Component[] components = slot.getComponents();
            for (Component c : components) {
                if (c instanceof JProgressBar hpBar) {
                    hpBar.setValue(stats.getCurrentHp());

                    if (stats.isFainted()) {
                        hpBar.setForeground(Color.GRAY);
                    } else if (stats.getHpPercentage() < 0.2) {
                        hpBar.setForeground(new Color(220, 20, 60));
                    } else if (stats.getHpPercentage() < 0.5) {
                        hpBar.setForeground(new Color(255, 165, 0));
                    } else {
                        hpBar.setForeground(new Color(76, 175, 80));
                    }
                    hpBar.repaint();
                }
            }

            slot.invalidate();
            slot.revalidate();
            slot.repaint();
        }
    }

    private void updateHealthBar(boolean isPlayer) {
        if (isPlayer) {
            PokemonBattleStats stats = playerTeam.getActivePokemon();
            int current = stats.getCurrentHp();
            int max = stats.getMaxHp();
            playerHealthBar.setValue(current);
            double percentage = stats.getHpPercentage() * 100;
            playerHealthBar.setString(String.format("%.0f%%", percentage));
            playerHPLabel.setText("HP: " + current + " / " + max);

            if (stats.getHpPercentage() < 0.2) {
                playerHealthBar.setForeground(new Color(220, 20, 60));
            } else if (stats.getHpPercentage() < 0.5) {
                playerHealthBar.setForeground(new Color(255, 165, 0));
            } else {
                playerHealthBar.setForeground(new Color(76, 175, 80));
            }
        } else {
            PokemonBattleStats stats = enemyTeam.getActivePokemon();
            int current = stats.getCurrentHp();
            int max = stats.getMaxHp();
            enemyHealthBar.setValue(current);
            double percentage = stats.getHpPercentage() * 100;
            enemyHealthBar.setString(String.format("%.0f%%", percentage));
            enemyHPLabel.setText("HP: " + current + " / " + max);

            if (stats.getHpPercentage() < 0.2) {
                enemyHealthBar.setForeground(new Color(220, 20, 60));
            } else if (stats.getHpPercentage() < 0.5) {
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

    @SuppressWarnings("UseSpecificCatch")
    private ImageIcon loadPokemonSprite(int id, boolean isEnemy) {
        String dir = isEnemy ? FRONT_IMAGE_DIR : BACK_IMAGE_DIR;
        String file = dir + id + ".gif";
        File f = new File(file);

        if (!f.exists()) {
            LOGGER.log(Level.WARNING, "Sprite not found: {0}", file);
            return createPlaceholderSprite();
        }

        try {
            java.net.URL url = f.toURI().toURL();
            ImageIcon originalIcon = new ImageIcon(url);

            int originalWidth = originalIcon.getIconWidth();
            int originalHeight = originalIcon.getIconHeight();

            if (originalWidth <= 0 || originalHeight <= 0) {
                LOGGER.log(Level.WARNING, "Invalid sprite dimensions: {0}", file);
                return createPlaceholderSprite();
            }

            int targetSize = 200;
            double scale = (double) targetSize / Math.max(originalWidth, originalHeight);
            int scaledWidth = (int) (originalWidth * scale);
            int scaledHeight = (int) (originalHeight * scale);

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
        parentFrame.setContentPane(new PokedexPanel(parentFrame, username));
        parentFrame.revalidate();
        parentFrame.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        GradientPaint gp = new GradientPaint(0, 0, new Color(45, 52, 90),
                                             0, getHeight(), new Color(88, 103, 168));
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        int groundY = getHeight() / 2 + 80;

        g2d.setColor(new Color(50, 80, 40, 80));
        g2d.fillRect(0, groundY - 5, getWidth(), 5);

        GradientPaint groundGradient = new GradientPaint(
            0, groundY, new Color(106, 168, 79, 120),
            0, getHeight(), new Color(78, 124, 58, 140));
        g2d.setPaint(groundGradient);
        g2d.fillRect(0, groundY, getWidth(), getHeight() - groundY);

        g2d.setColor(new Color(78, 124, 58, 80));
        for (int i = 0; i < 8; i++) {
            int y = groundY + (i * 40);
            if (y < getHeight()) {
                int lineThickness = 1 + (i / 3);
                for (int t = 0; t < lineThickness; t++) {
                    g2d.drawLine(0, y + t, getWidth(), y + t);
                }
            }
        }

        g2d.setColor(new Color(78, 124, 58, 40));
        int numVerticalLines = 12;
        for (int i = 1; i < numVerticalLines; i++) {
            int x = (getWidth() * i) / numVerticalLines;
            g2d.drawLine(x, groundY, x, getHeight());
        }
    }
}
