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
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

import backend.application.dto.BattleStateDTO;
import backend.application.dto.PokemonDTO;
import backend.domain.model.Move;
import backend.infrastructure.network.BattleClient;
import backend.infrastructure.network.NetworkProtocol.BattleOutcomeType;
import shared.util.I18n;

/**
 * Multiplayer Battle Panel - Network-enabled real-time battles
 * Similar to SingleplayerBattlePanel but synchronized over network
 */
public class MultiplayerBattlePanel extends JPanel implements BattleClient.BattleClientListener {
    private static final Logger LOGGER = Logger.getLogger(MultiplayerBattlePanel.class.getName());
    private static final String BACK_IMAGE_DIR = "Images/Back-Pokemon-gif/";
    private static final String FRONT_IMAGE_DIR = "Images/Front-Pokemon-gif/";
    private static final String ICON_IMAGE_DIR = "Images/Image-Pokedex/";
        private static final Color DISABLED_BUTTON_COLOR = new Color(120, 120, 120);
        private static final Color SLOT_HEALTHY_COLOR = new Color(255, 255, 255, 230);
        private static final Color SLOT_FAINTED_COLOR = new Color(60, 60, 60, 200);
    
    private final BattleClient client;
    private final boolean isPlayerOne;
    private final String username;
    private final JFrame parentFrame;
    private String opponentUsername;
    
    // Battle state
    private BattleStateDTO currentState;
    private List<Move> playerMoves;
    private boolean isMyTurn;
    private boolean isProcessing = false;
    
    // UI Components
    private JLabel playerNameLabel, enemyNameLabel;
    private JLabel playerHPLabel, enemyHPLabel;
    private JProgressBar playerHealthBar, enemyHealthBar;
    private JLabel battleMessageLabel;
    private JButton[] attackButtons;
    private JButton switchButton;
    private JButton forfeitButton;
    private boolean forcedSwitchRequired = false;
    private JDialog forcedSwitchDialog;
    private JLabel playerSpriteLabel, enemySpriteLabel;
    private JLabel turnIndicatorLabel;
    private JLabel myTeamTitleLabel;
    private JLabel opponentTeamTitleLabel;
    private boolean outcomeDialogShown = false;
    private javax.swing.Timer attackAnimationTimer;
    // Team bar components for live updates
    private final JProgressBar[] playerTeamHpBars;
    private final JProgressBar[] enemyTeamHpBars;
    private final JLabel[] playerTeamActiveIndicators;
    private final JLabel[] enemyTeamActiveIndicators;
    private final JPanel[] playerTeamSlotPanels;
    private final JPanel[] enemyTeamSlotPanels;
    
    public MultiplayerBattlePanel(BattleClient client, boolean isPlayerOne, 
                                 String username, String opponentUsername,
                                 JFrame parentFrame, BattleStateDTO initialState) {
        this.client = client;
        this.isPlayerOne = isPlayerOne;
        this.username = username;
        this.parentFrame = parentFrame;
        this.opponentUsername = opponentUsername;
        this.currentState = initialState; // Initialize state BEFORE creating UI
        
        // Initialize arrays
        this.attackButtons = new JButton[4];
        this.playerTeamHpBars = new JProgressBar[6];
        this.enemyTeamHpBars = new JProgressBar[6];
        this.playerTeamActiveIndicators = new JLabel[6];
        this.enemyTeamActiveIndicators = new JLabel[6];
        this.playerTeamSlotPanels = new JPanel[6];
        this.enemyTeamSlotPanels = new JPanel[6];
        
        // Load moves from server-provided state
        loadMovesFromState();
        
        initializeUI();
        bootstrapInitialState();
        updateTeamTitleLabels();

        SwingUtilities.invokeLater(() -> client.setListener(MultiplayerBattlePanel.this));
    }

    private void bootstrapInitialState() {
        if (currentState == null) {
            return;
        }
        updateBattleState(currentState);
        startBattle();
    }
    
    private void loadMovesFromState() {
        // Get moves from current active Pokemon provided by server
        if (currentState == null) {
            playerMoves = new ArrayList<>();
            return;
        }
        PokemonDTO activePokemon = getCurrentPlayerPokemon();
        LOGGER.info(() -> "Loading moves for " + activePokemon.getName());
        LOGGER.info(() -> "Available moves provided: " + 
            (activePokemon.getAvailableMoves() != null ? activePokemon.getAvailableMoves().size() : "null"));
        
        if (activePokemon.getAvailableMoves() != null && !activePokemon.getAvailableMoves().isEmpty()) {
            playerMoves = new ArrayList<>();
            for (backend.application.dto.MoveDTO moveDTO : activePokemon.getAvailableMoves()) {
                final String moveName = moveDTO.getName();
                LOGGER.fine(() -> "Registering move: " + moveName);
                playerMoves.add(new Move(
                    moveDTO.getName(),
                    moveDTO.getType(),
                    moveDTO.getPower(),
                    moveDTO.getAccuracy()
                ));
            }
        } else {
            LOGGER.warning("Server provided no moves, using fallback move set");
            // Fallback to temporary moves if server didn't provide any
            playerMoves = new ArrayList<>(java.util.Arrays.asList(
                new Move("Tackle", "Normal", 40, 100),
                new Move("Scratch", "Normal", 40, 100),
                new Move("Quick Attack", "Normal", 40, 100),
                new Move("Hyper Beam", "Normal", 150, 90)
            ));
        }

        if (playerMoves == null) {
            playerMoves = new ArrayList<>();
        }
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(45, 52, 90));
        setDoubleBuffered(true);
        
        JPanel mainContainer = new JPanel(new BorderLayout(0, 0));
        mainContainer.setOpaque(false);
        
        // FIXED: Both players see green bar (left) with MY team, red bar (right) with OPPONENT team
        // Green bar on left = My team (always)
        JPanel leftTeamPanel = createVerticalTeamBar(true);  // My team - green
        mainContainer.add(leftTeamPanel, BorderLayout.WEST);
        
        // Red bar on right = Opponent team (always)
        JPanel rightTeamPanel = createVerticalTeamBar(false); // Opponent team - red
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
    
    private JPanel createCompactInfoCard(boolean isPlayer) {
        PokemonDTO pokemon = isPlayer ? getCurrentPlayerPokemon() : getCurrentEnemyPokemon();
        
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
        
        JLabel levelLabel = new JLabel(I18n.get("battle.label.level"));
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
        
        JLabel hpLabel = new JLabel(I18n.get("battle.label.hp", pokemon.getCurrentHp(), pokemon.getMaxHp()));
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
        
        JProgressBar healthBar = new JProgressBar(0, pokemon.getMaxHp());
        healthBar.setValue(pokemon.getCurrentHp());
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
        
        // Add type badges
        card.add(Box.createVerticalStrut(2));
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        typePanel.setOpaque(false);
        typePanel.add(PokemonUtils.createTypeBadge(pokemon.getType1()));
        if (pokemon.getType2() != null && !pokemon.getType2().isEmpty() && !pokemon.getType2().equals("None")) {
            typePanel.add(PokemonUtils.createTypeBadge(pokemon.getType2()));
        }
        typePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(typePanel);
        
        return card;
    }
    
    private JPanel createVerticalTeamBar(boolean isMyTeam) {
        // FIXED: Use perspective-based helpers - isMyTeam=true shows MY Pokemon, false shows OPPONENT
        List<PokemonDTO> team = isMyTeam ? getMyTeam() : getOpponentTeam();
        int activeIndex = isMyTeam ? getMyActiveIndex() : getOpponentActiveIndex();
        
        JPanel barPanel = new JPanel();
        barPanel.setLayout(new BoxLayout(barPanel, BoxLayout.Y_AXIS));
        barPanel.setOpaque(true);
        barPanel.setBackground(new Color(0, 0, 0, 200));
        barPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(isMyTeam ? new Color(76, 175, 80) : new Color(220, 20, 60), 3),
            BorderFactory.createEmptyBorder(12, 8, 12, 8)
        ));
        barPanel.setPreferredSize(new Dimension(110, 0));
        
        JLabel titleLabel = new JLabel(buildTeamLabel(isMyTeam), SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 11));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        if (isMyTeam) {
            myTeamTitleLabel = titleLabel;
        } else {
            opponentTeamTitleLabel = titleLabel;
        }
        barPanel.add(titleLabel);
        barPanel.add(Box.createVerticalStrut(12));
        
        
        for (int i = 0; i < team.size(); i++) {
            PokemonDTO pokemon = team.get(i);
            
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
            slot.setBackground(SLOT_HEALTHY_COLOR);
            slot.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60), 2, true),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));
            
            JLabel iconLabel = new JLabel();
            iconLabel.setOpaque(false);
            iconLabel.setDoubleBuffered(true);
            iconLabel.setIcon(loadPokemonSmallIcon(pokemon.getId()));
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            slot.add(iconLabel);
            slot.add(Box.createVerticalStrut(4));
            
            String displayName = pokemon.getName().length() > 8 ?
                pokemon.getName().substring(0, 7) + "." : pokemon.getName();
            JLabel nameLabel = new JLabel(displayName, SwingConstants.CENTER);
            nameLabel.setFont(new Font("Arial", Font.BOLD, 9));
            nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            slot.add(nameLabel);
            slot.add(Box.createVerticalStrut(3));
            
            JProgressBar hpBar = new JProgressBar(0, pokemon.getMaxHp());
            hpBar.setValue(pokemon.getCurrentHp());
            hpBar.setPreferredSize(new Dimension(70, 8));
            hpBar.setMaximumSize(new Dimension(70, 8));
            hpBar.setForeground(new Color(76, 175, 80));
            hpBar.setBackground(new Color(220, 220, 220));
            hpBar.setBorderPainted(true);
            hpBar.setAlignmentX(Component.CENTER_ALIGNMENT);
            slot.add(hpBar);
            
            // Cache HP bar for updates
            if (isMyTeam) {
                playerTeamHpBars[i] = hpBar;
                playerTeamSlotPanels[i] = slot;
            } else {
                enemyTeamHpBars[i] = hpBar;
                enemyTeamSlotPanels[i] = slot;
            }
            
            JLabel activeIndicator = new JLabel(I18n.get("battle.label.active"), SwingConstants.CENTER);
            activeIndicator.setFont(new Font("Arial", Font.BOLD, 8));
            activeIndicator.setForeground(new Color(255, 215, 0));
            activeIndicator.setAlignmentX(Component.CENTER_ALIGNMENT);
            activeIndicator.setVisible(i == activeIndex);
            slot.add(Box.createVerticalStrut(2));
            slot.add(activeIndicator);
            
            // Cache active indicator for updates
            if (isMyTeam) {
                playerTeamActiveIndicators[i] = activeIndicator;
            } else {
                enemyTeamActiveIndicators[i] = activeIndicator;
            }
            
            slotContainer.add(slot, BorderLayout.CENTER);
            barPanel.add(slotContainer);
            
            if (i < team.size() - 1) {
                barPanel.add(Box.createVerticalStrut(10));
            }
        }
        
        
        barPanel.add(Box.createVerticalGlue());
        return barPanel;
    }

    private void updateTeamTitleLabels() {
        if (myTeamTitleLabel != null) {
            myTeamTitleLabel.setText(buildTeamLabel(true));
        }
        if (opponentTeamTitleLabel != null) {
            opponentTeamTitleLabel.setText(buildTeamLabel(false));
        }
    }

    private String buildTeamLabel(boolean isMyTeam) {
        String ownerName = isMyTeam ? username : opponentUsername;
        if (ownerName == null || ownerName.isBlank()) {
            return I18n.get(isMyTeam ? "battle.team.your" : "battle.team.waiting");
        }
        return I18n.get("battle.team.named", ownerName);
    }
    
    private JPanel createBattleFieldPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Enemy sprite
        enemySpriteLabel = new JLabel();
        enemySpriteLabel.setOpaque(false);
        enemySpriteLabel.setIcon(loadPokemonSprite(getCurrentEnemyPokemon().getId(), true));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.weighty = 0.55;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        gbc.insets = new Insets(10, 20, 0, 30);
        panel.add(enemySpriteLabel, gbc);
        
        // Player sprite
        playerSpriteLabel = new JLabel();
        playerSpriteLabel.setOpaque(false);
        playerSpriteLabel.setIcon(loadPokemonSprite(getCurrentPlayerPokemon().getId(), false));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 0.45;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        gbc.insets = new Insets(0, 30, 10, 20);
        panel.add(playerSpriteLabel, gbc);
        
        // Message panel
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

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        statusPanel.setOpaque(false);
        turnIndicatorLabel = new JLabel(I18n.get("battle.multiplayer.waiting"));
        turnIndicatorLabel.setFont(new Font("Arial", Font.BOLD, 16));
        turnIndicatorLabel.setForeground(Color.LIGHT_GRAY);
        statusPanel.add(turnIndicatorLabel);
        panel.add(statusPanel, BorderLayout.NORTH);
        
        JPanel attackPanel = new JPanel(new GridLayout(2, 2, 6, 6));
        attackPanel.setOpaque(false);
        
        attackButtons = new JButton[4];
        for (int i = 0; i < 4; i++) {
            final int index = i;
            JButton button;
            if (playerMoves != null && i < playerMoves.size()) {
                Move move = playerMoves.get(i);
                button = PokemonUtils.createAttackButton(move, e -> sendMove(index));
            } else {
                button = PokemonUtils.createActionButton("—", new Color(90, 90, 90));
                button.addActionListener(e -> sendMove(index));
            }
            button.setEnabled(false);
            button.putClientProperty("disabledColor", DISABLED_BUTTON_COLOR);
            attackButtons[i] = button;
            attackPanel.add(button);
        }
        updateAttackButtons();
        
        panel.add(attackPanel, BorderLayout.CENTER);
        
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        actionPanel.setOpaque(false);
        
        switchButton = PokemonUtils.createActionButton(I18n.get("battle.button.switch"), new Color(52, 152, 219));
        switchButton.addActionListener(e -> showSwitchDialog());
        switchButton.setEnabled(false);
        actionPanel.add(switchButton);

        forfeitButton = PokemonUtils.createActionButton(I18n.get("battle.button.forfeit"), new Color(231, 76, 60));
        forfeitButton.addActionListener(e -> promptForfeit());
        actionPanel.add(forfeitButton);
        
        panel.add(actionPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void startBattle() {
        showBattleMessage(I18n.get("battle.message.trainerWants"), 2000, () -> {
            checkTurnAndEnableControls();
        });
    }
    
    private void sendMove(int moveIndex) {
        if (!isMyTurn || client == null || isProcessing) return;
        
        isProcessing = true;
        disableControls();
        
        // Show attack message like SingleplayerBattlePanel
        if (playerMoves != null && moveIndex < playerMoves.size()) {
            PokemonDTO myPokemon = getCurrentPlayerPokemon();
            Move move = playerMoves.get(moveIndex);
            showBattleMessage(I18n.get("battle.message.used", myPokemon.getName(), move.getLocalizedName()), 1500, () -> {
                client.sendMove(moveIndex);
                showBattleMessage(I18n.get("battle.multiplayer.waiting"), 0, null);
            });
        } else {
            client.sendMove(moveIndex);
            showBattleMessage(I18n.get("battle.multiplayer.waiting"), 0, null);
        }
    }
    
    private void showSwitchDialog() {
        if (!isMyTurn) return;
        if (forcedSwitchRequired) {
            showForcedSwitchDialog();
            return;
        }
        
        // Create switch dialog similar to SingleplayerBattlePanel
        JDialog dialog = new JDialog(parentFrame, I18n.get("battle.dialog.switch"), true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(450, 550);
        dialog.setLocationRelativeTo(this);
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        contentPanel.setOpaque(false);
        
        // FIXED: Use perspective-based helpers instead of conditionals
        List<PokemonDTO> team = getMyTeam();
        if (team.isEmpty()) {
            dialog.dispose();
            return;
        }
        int activeIndex = getMyActiveIndex();
        
        for (int i = 0; i < team.size(); i++) {
            if (i == activeIndex) continue;
            
            final int index = i;
            PokemonDTO pokemon = team.get(i);
            
            // FIXED: Use currentHp instead of base hp stat, check fainted flag
            if (pokemon.isFainted() || pokemon.getCurrentHp() <= 0) continue; // Skip fainted
            
            JPanel pokemonPanel = createPokemonSwitchCard(pokemon, index, dialog);
            contentPanel.add(pokemonPanel);
            contentPanel.add(Box.createVerticalStrut(10));
        }
        
        JButton cancelButton = PokemonUtils.createActionButton(I18n.get("battle.button.cancel"), new Color(231, 76, 60));
        cancelButton.addActionListener(e -> dialog.dispose());
        cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(cancelButton);
        
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        dialog.add(scrollPane);
        
        dialog.setVisible(true);
    }
    
    private JPanel createPokemonSwitchCard(PokemonDTO pokemon, int index, JDialog dialog) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(76, 175, 80), 3, true),
            new EmptyBorder(12, 12, 12, 12)
        ));
        panel.setBackground(new Color(240, 255, 240));
        panel.setMaximumSize(new Dimension(400, 90));
        
        JLabel iconLabel = new JLabel(loadPokemonSmallIcon(pokemon.getId()));
        panel.add(iconLabel, BorderLayout.WEST);
        
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        
        JLabel nameLabel = new JLabel(pokemon.getName());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        infoPanel.add(nameLabel);
        
        JLabel hpLabel = new JLabel(I18n.get("battle.label.hp", pokemon.getCurrentHp(), pokemon.getMaxHp()));
        hpLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        infoPanel.add(hpLabel);
        
        panel.add(infoPanel, BorderLayout.CENTER);
        
        panel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleSwitchSelection(index, dialog);
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(255, 215, 0), 4, true),
                    new EmptyBorder(12, 12, 12, 12)
                ));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(76, 175, 80), 3, true),
                    new EmptyBorder(12, 12, 12, 12)
                ));
            }
        });
        
        return panel;
    }

    private void handleSwitchSelection(int index, JDialog dialog) {
        if (dialog != null) {
            dialog.dispose();
            if (dialog == forcedSwitchDialog) {
                forcedSwitchDialog = null;
            }
        }

        forcedSwitchRequired = false;
        disableControls();
        isProcessing = true;

        if (client != null) {
            client.sendSwitchPokemon(index);
        }
        showBattleMessage(I18n.get("battle.multiplayer.waiting"), 0, null);
    }

    private void evaluateForcedSwitchRequirement() {
        PokemonDTO myPokemon = getCurrentPlayerPokemon();
        boolean fainted = myPokemon == null || myPokemon.isFainted() || myPokemon.getCurrentHp() <= 0;
        boolean canSwitch = fainted && hasHealthyBenchPokemon();

        if (canSwitch) {
            if (!forcedSwitchRequired) {
                showBattleMessage(I18n.get("battle.dialog.switchMandatory"), 0, null);
            }
            forcedSwitchRequired = true;
            isProcessing = true;
            disableControls();
            turnIndicatorLabel.setText(I18n.get("battle.dialog.switchMandatory"));
            turnIndicatorLabel.setForeground(new Color(255, 120, 120));

            if (forcedSwitchDialog == null || !forcedSwitchDialog.isVisible()) {
                SwingUtilities.invokeLater(this::showForcedSwitchDialog);
            }
        } else {
            if (forcedSwitchRequired) {
                closeForcedSwitchDialog();
            }
            forcedSwitchRequired = false;
        }
    }

    private boolean hasHealthyBenchPokemon() {
        List<PokemonDTO> team = getMyTeam();
        if (team == null || team.isEmpty()) {
            return false;
        }
        int activeIndex = getMyActiveIndex();
        for (int i = 0; i < team.size(); i++) {
            if (i == activeIndex) {
                continue;
            }
            PokemonDTO pokemon = team.get(i);
            if (pokemon == null) {
                continue;
            }
            if (!pokemon.isFainted() && pokemon.getCurrentHp() > 0) {
                return true;
            }
        }
        return false;
    }

    private void showForcedSwitchDialog() {
        if (!forcedSwitchRequired || !hasHealthyBenchPokemon()) {
            return;
        }

        List<PokemonDTO> team = getMyTeam();
        if (team == null) {
            return;
        }

        forcedSwitchDialog = new JDialog(parentFrame, I18n.get("battle.dialog.switchMandatory"), true);
        forcedSwitchDialog.setLayout(new BorderLayout(10, 10));
        forcedSwitchDialog.setSize(450, 550);
        forcedSwitchDialog.setLocationRelativeTo(this);
        forcedSwitchDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        contentPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(I18n.get("battle.dialog.mustChoose"));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(new Color(255, 120, 120));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(6));

        JLabel subtitleLabel = new JLabel(I18n.get("battle.dialog.clickHealthy"));
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(Color.WHITE);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(subtitleLabel);
        contentPanel.add(Box.createVerticalStrut(12));

        int activeIndex = getMyActiveIndex();
        for (int i = 0; i < team.size(); i++) {
            if (i == activeIndex) {
                continue;
            }
            PokemonDTO pokemon = team.get(i);
            if (pokemon == null) {
                continue;
            }
            if (pokemon.isFainted() || pokemon.getCurrentHp() <= 0) {
                continue;
            }

            JPanel pokemonPanel = createPokemonSwitchCard(pokemon, i, forcedSwitchDialog);
            contentPanel.add(pokemonPanel);
            contentPanel.add(Box.createVerticalStrut(10));
        }

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        forcedSwitchDialog.add(scrollPane);

        forcedSwitchDialog.setVisible(true);
    }

    private void closeForcedSwitchDialog() {
        if (forcedSwitchDialog != null) {
            forcedSwitchDialog.dispose();
            forcedSwitchDialog = null;
        }
    }
    
    private void updateBattleState(BattleStateDTO state) {
        if (state == null) {
            return;
        }
        this.currentState = state;

        // Update player info
        updatePokemonInfo(true);
        updatePokemonInfo(false);

        // Update team bars
        updateTeamBars();

        // Update sprites
        playerSpriteLabel.setIcon(loadPokemonSprite(getCurrentPlayerPokemon().getId(), false));
        enemySpriteLabel.setIcon(loadPokemonSprite(getCurrentEnemyPokemon().getId(), true));

        // Reload moves for new active Pokemon
        loadMovesFromState();

        // Update attack buttons
        updateAttackButtons();

        checkTurnAndEnableControls();
        evaluateForcedSwitchRequirement();

        // If battle ended, lock UI (final dialog handled via network callback)
        if (state.isBattleEnded()) {
            disableControls();
            setForfeitButtonEnabled(false);
        }
    }
    
    private void updatePokemonInfo(boolean isPlayer) {
        PokemonDTO pokemon = isPlayer ? getCurrentPlayerPokemon() : getCurrentEnemyPokemon();
        if (pokemon == null) {
            return;
        }
        JLabel nameLabel = isPlayer ? playerNameLabel : enemyNameLabel;
        JLabel hpLabel = isPlayer ? playerHPLabel : enemyHPLabel;
        JProgressBar healthBar = isPlayer ? playerHealthBar : enemyHealthBar;
        
        nameLabel.setText(pokemon.getName().toUpperCase());
        hpLabel.setText(I18n.get("battle.label.hp", pokemon.getCurrentHp(), pokemon.getMaxHp()));
        healthBar.setMaximum(pokemon.getMaxHp());
        healthBar.setValue(pokemon.getCurrentHp());
        
        double hpPercent = pokemon.getMaxHp() > 0 ? (double) pokemon.getCurrentHp() / pokemon.getMaxHp() : 0;
        healthBar.setString(String.format("%.0f%%", hpPercent * 100));
        
        if (hpPercent < 0.2) {
            healthBar.setForeground(new Color(220, 20, 60));
        } else if (hpPercent < 0.5) {
            healthBar.setForeground(new Color(255, 165, 0));
        } else {
            healthBar.setForeground(new Color(76, 175, 80));
        }
    }
    
    private void updateTeamBars() {
        updateTeamBarEntries(
            getMyTeam(),
            playerTeamHpBars,
            playerTeamActiveIndicators,
            getMyActiveIndex(),
            playerTeamSlotPanels
        );
        updateTeamBarEntries(
            getOpponentTeam(),
            enemyTeamHpBars,
            enemyTeamActiveIndicators,
            getOpponentActiveIndex(),
            enemyTeamSlotPanels
        );
    }

    private void updateTeamBarEntries(List<PokemonDTO> team,
                                      JProgressBar[] hpBars,
                                      JLabel[] activeIndicators,
                                      int activeIndex,
                                      JPanel[] slotPanels) {
        for (int i = 0; i < hpBars.length; i++) {
            PokemonDTO pokemon = null;
            if (team != null && i < team.size()) {
                pokemon = team.get(i);
            }
            boolean hasPokemon = pokemon != null;

            if (hpBars[i] != null) {
                if (hasPokemon && pokemon != null) {
                    hpBars[i].setMaximum(pokemon.getMaxHp());
                    hpBars[i].setValue(Math.max(0, pokemon.getCurrentHp()));

                    double hpPercent = pokemon.getMaxHp() > 0
                        ? (double) pokemon.getCurrentHp() / pokemon.getMaxHp()
                        : 0;

                    if (hpPercent < 0.2) {
                        hpBars[i].setForeground(new Color(220, 20, 60));
                    } else if (hpPercent < 0.5) {
                        hpBars[i].setForeground(new Color(255, 165, 0));
                    } else {
                        hpBars[i].setForeground(new Color(76, 175, 80));
                    }
                } else {
                    hpBars[i].setValue(0);
                    hpBars[i].setForeground(new Color(60, 60, 60));
                }
            }

            if (activeIndicators[i] != null) {
                activeIndicators[i].setVisible(hasPokemon && i == activeIndex);
            }

            updateSlotAppearance(slotPanels, i, pokemon);
        }
    }

    private void updateSlotAppearance(JPanel[] slots, int index, PokemonDTO pokemon) {
        if (slots == null || index < 0 || index >= slots.length) {
            return;
        }
        JPanel slot = slots[index];
        if (slot == null) {
            return;
        }

        boolean fainted = pokemon == null || pokemon.isFainted() || pokemon.getCurrentHp() <= 0;
        Color background = fainted ? SLOT_FAINTED_COLOR : SLOT_HEALTHY_COLOR;
        Color textColor = fainted ? new Color(200, 200, 200) : Color.BLACK;

        slot.setBackground(background);
        slot.repaint();

        Component[] children = slot.getComponents();
        for (Component child : children) {
            if (child instanceof JLabel label) {
                label.setForeground(textColor);
            }
        }
    }

    private enum AttackAnimationDirection {
        PLAYER,
        ENEMY,
        NONE
    }

    private void syncAttackButtonColors(JButton button) {
        if (button == null) {
            return;
        }
        boolean enabled = button.isEnabled();
        Color color = (Color) button.getClientProperty(enabled ? "originalColor" : "disabledColor");
        if (color == null) {
            color = enabled ? button.getBackground() : DISABLED_BUTTON_COLOR;
        }
        button.setBackground(color);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(enabled ? color.darker().darker() : new Color(80, 80, 80), 3, true),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
    }

    private void applyActionButtonState(JButton button, boolean enabled) {
        if (button == null) {
            return;
        }
        button.setEnabled(enabled);
        Color color = (Color) button.getClientProperty(enabled ? "originalColor" : "disabledColor");
        if (color == null) {
            color = enabled ? button.getBackground() : DISABLED_BUTTON_COLOR;
        }
        button.setBackground(color);
    }
    
    private void updateAttackButtons() {
        if (attackButtons == null) {
            return;
        }

        for (int i = 0; i < attackButtons.length; i++) {
            JButton button = attackButtons[i];
            if (button == null) {
                continue;
            }

            if (playerMoves != null && i < playerMoves.size()) {
                Move move = playerMoves.get(i);
                PokemonUtils.updateAttackButton(button, move);
                button.putClientProperty("disabledColor", DISABLED_BUTTON_COLOR);
            } else {
                button.setText("—");
                button.putClientProperty("originalColor", new Color(90, 90, 90));
                button.putClientProperty("disabledColor", DISABLED_BUTTON_COLOR);
                button.setEnabled(false);
            }

            syncAttackButtonColors(button);
        }
    }
    
    private void checkTurnAndEnableControls() {
        String currentTurn = currentState.getCurrentTurn();
        boolean playerTurn = (isPlayerOne && "PLAYER".equals(currentTurn)) || 
                           (!isPlayerOne && "ENEMY".equals(currentTurn));
        
        isMyTurn = playerTurn;
        
        if (isMyTurn && !isProcessing) {
            PokemonDTO myPokemon = getCurrentPlayerPokemon();
            turnIndicatorLabel.setText(I18n.get("battle.multiplayer.yourTurn"));
            turnIndicatorLabel.setForeground(Color.GREEN);
            
            // Show "What will X do?" message like SingleplayerBattlePanel
            showBattleMessage(I18n.get("battle.message.whatWillDo", myPokemon.getName()), 1000, () -> {
                enableControls();
            });
        } else {
            turnIndicatorLabel.setText(I18n.get("battle.multiplayer.opponentTurn"));
            turnIndicatorLabel.setForeground(Color.ORANGE);
            disableControls();
            if (!isProcessing) {
                showBattleMessage(I18n.get("battle.multiplayer.waiting"), 0, null);
            }
        }
    }
    
    private void enableControls() {
        for (JButton button : attackButtons) {
            if (button == null) {
                continue;
            }
            button.setEnabled(true);
            syncAttackButtonColors(button);
        }
        applyActionButtonState(switchButton, true);
    }
    
    private void disableControls() {
        for (JButton button : attackButtons) {
            if (button == null) {
                continue;
            }
            button.setEnabled(false);
            syncAttackButtonColors(button);
        }
        applyActionButtonState(switchButton, false);
    }

    private void setForfeitButtonEnabled(boolean enabled) {
        if (forfeitButton != null) {
            applyActionButtonState(forfeitButton, enabled);
        }
    }
    
    private void showBattleMessage(String message, int duration, Runnable onComplete) {
        battleMessageLabel.setText(message);
        battleMessageLabel.setVisible(true);
        
        // Force repaint to show message immediately
        if (battleMessageLabel.getParent() != null) {
            battleMessageLabel.getParent().repaint();
        }
        
        if (duration > 0 && onComplete != null) {
            javax.swing.Timer timer = new javax.swing.Timer(duration, e -> onComplete.run());
            timer.setRepeats(false);
            timer.start();
        }
    }

    private void animateAttack(boolean isPlayerAttacking, Runnable onComplete) {
        if (playerSpriteLabel == null || enemySpriteLabel == null) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        if (attackAnimationTimer != null && attackAnimationTimer.isRunning()) {
            attackAnimationTimer.stop();
        }

        final int[] frame = {0};
        final int maxFrames = 15;

        JLabel attackingSprite = isPlayerAttacking ? playerSpriteLabel : enemySpriteLabel;
        JLabel defendingSprite = isPlayerAttacking ? enemySpriteLabel : playerSpriteLabel;

        Point originalPosAtk = attackingSprite.getLocation();
        Point originalPosDef = defendingSprite.getLocation();

        attackAnimationTimer = new javax.swing.Timer(35, null);
        attackAnimationTimer.addActionListener(e -> {
            frame[0]++;

            if (frame[0] <= 5) {
                int offset = (frame[0] % 2 == 0) ? 15 : -15;
                attackingSprite.setLocation(originalPosAtk.x + offset, originalPosAtk.y);
            } else if (frame[0] <= 10) {
                attackingSprite.setLocation(originalPosAtk);
                boolean blink = frame[0] % 2 == 0;
                defendingSprite.setVisible(!blink);
                int offset = blink ? 12 : -12;
                defendingSprite.setLocation(originalPosDef.x + offset, originalPosDef.y);
            } else {
                defendingSprite.setVisible(true);
                defendingSprite.setLocation(originalPosDef);
            }

            if (frame[0] >= maxFrames) {
                attackAnimationTimer.stop();
                defendingSprite.setVisible(true);
                attackingSprite.setLocation(originalPosAtk);
                defendingSprite.setLocation(originalPosDef);
                if (onComplete != null) {
                    SwingUtilities.invokeLater(onComplete);
                }
            }
        });
        attackAnimationTimer.start();
    }

    private void showBattleOutcomeDialog(boolean didIWin,
                                         String winnerName,
                                         String loserName,
                                         BattleOutcomeType outcomeType) {
        if (outcomeDialogShown) {
            return;
        }
        outcomeDialogShown = true;

        disableControls();
        setForfeitButtonEnabled(false);
        closeForcedSwitchDialog();

        String safeWinner = resolveTrainerName(winnerName, didIWin);
        String safeLoser = resolveTrainerName(loserName, !didIWin);
        String titleKey = didIWin ? "battle.outcome.title.victory" : "battle.outcome.title.defeat";
        String messageKey = resolveOutcomeMessageKey(didIWin, outcomeType);

        JOptionPane.showMessageDialog(
            this,
            I18n.get(messageKey, safeWinner, safeLoser),
            I18n.get(titleKey),
            didIWin ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE
        );

        returnToPokedex();
    }

    private String resolveOutcomeMessageKey(boolean didIWin, BattleOutcomeType outcomeType) {
        BattleOutcomeType type = outcomeType != null ? outcomeType : BattleOutcomeType.NORMAL;
        String prefix = didIWin ? "battle.outcome.victory." : "battle.outcome.defeat.";
        return switch (type) {
            case FORFEIT -> prefix + "forfeit";
            case DISCONNECT -> prefix + "disconnect";
            default -> prefix + "normal";
        };
    }

    private String resolveTrainerName(String providedName, boolean isPlayer) {
        if (providedName != null && !providedName.isBlank()) {
            return providedName;
        }
        String fallback = isPlayer ? username : opponentUsername;
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return I18n.get("battle.outcome.unknown");
    }
    
    private void returnToPokedex() {
        if (client != null) {
            client.disconnect();
        }
        parentFrame.getContentPane().removeAll();
        parentFrame.setContentPane(new PokedexPanel(parentFrame, username));
        parentFrame.revalidate();
        parentFrame.repaint();
    }

    private void promptForfeit() {
        if (client == null) {
            return;
        }

        int option = JOptionPane.showConfirmDialog(
            parentFrame,
            I18n.get("battle.forfeit.confirm"),
            I18n.get("battle.button.forfeit"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (option == JOptionPane.YES_OPTION) {
            disableControls();
            setForfeitButtonEnabled(false);
            showBattleMessage(I18n.get("battle.forfeit.message"), 0, null);
            isProcessing = true;
            client.sendForfeit(username + " forfeited");
        }
    }
    
    private PokemonDTO getCurrentPlayerPokemon() {
        List<PokemonDTO> team = getMyTeam();
        if (team == null || team.isEmpty()) {
            return null;
        }
        int index = Math.max(0, Math.min(getMyActiveIndex(), team.size() - 1));
        return team.get(index);
    }
    
    private PokemonDTO getCurrentEnemyPokemon() {
        List<PokemonDTO> team = getOpponentTeam();
        if (team == null || team.isEmpty()) {
            return null;
        }
        int index = Math.max(0, Math.min(getOpponentActiveIndex(), team.size() - 1));
        return team.get(index);
    }
    
    // FIXED: Perspective-based helpers for proper mirroring
    // These always return "my team" and "opponent team" from current player's view
    private List<PokemonDTO> getMyTeam() {
        if (currentState == null) {
            return Collections.emptyList();
        }
        List<PokemonDTO> team = isPlayerOne ? currentState.getPlayerTeam() : currentState.getEnemyTeam();
        return team != null ? team : Collections.emptyList();
    }
    
    private List<PokemonDTO> getOpponentTeam() {
        if (currentState == null) {
            return Collections.emptyList();
        }
        List<PokemonDTO> team = isPlayerOne ? currentState.getEnemyTeam() : currentState.getPlayerTeam();
        return team != null ? team : Collections.emptyList();
    }
    
    private int getMyActiveIndex() {
        if (currentState == null) {
            return 0;
        }
        return isPlayerOne ? currentState.getPlayerActivePokemonIndex() : currentState.getEnemyActivePokemonIndex();
    }
    
    private int getOpponentActiveIndex() {
        if (currentState == null) {
            return 0;
        }
        return isPlayerOne ? currentState.getEnemyActivePokemonIndex() : currentState.getPlayerActivePokemonIndex();
    }

    private PokemonDTO getActivePokemonFromState(BattleStateDTO state, boolean isPlayerPerspective) {
        if (state == null) {
            return null;
        }
        List<PokemonDTO> team = isPlayerPerspective
            ? (isPlayerOne ? state.getPlayerTeam() : state.getEnemyTeam())
            : (isPlayerOne ? state.getEnemyTeam() : state.getPlayerTeam());
        if (team == null || team.isEmpty()) {
            return null;
        }
        int index = isPlayerPerspective
            ? (isPlayerOne ? state.getPlayerActivePokemonIndex() : state.getEnemyActivePokemonIndex())
            : (isPlayerOne ? state.getEnemyActivePokemonIndex() : state.getPlayerActivePokemonIndex());
        if (index < 0 || index >= team.size()) {
            return null;
        }
        return team.get(index);
    }

    private AttackAnimationDirection determineAttackAnimation(BattleStateDTO nextState) {
        if (currentState == null || nextState == null) {
            return AttackAnimationDirection.NONE;
        }

        PokemonDTO previousMyPokemon = getActivePokemonFromState(currentState, true);
        PokemonDTO previousEnemyPokemon = getActivePokemonFromState(currentState, false);
        PokemonDTO updatedMyPokemon = getActivePokemonFromState(nextState, true);
        PokemonDTO updatedEnemyPokemon = getActivePokemonFromState(nextState, false);

        if (previousMyPokemon == null || previousEnemyPokemon == null ||
            updatedMyPokemon == null || updatedEnemyPokemon == null) {
            return AttackAnimationDirection.NONE;
        }

        if (previousMyPokemon.getId() != updatedMyPokemon.getId() ||
            previousEnemyPokemon.getId() != updatedEnemyPokemon.getId()) {
            return AttackAnimationDirection.NONE;
        }

        int myHpDelta = updatedMyPokemon.getCurrentHp() - previousMyPokemon.getCurrentHp();
        int enemyHpDelta = updatedEnemyPokemon.getCurrentHp() - previousEnemyPokemon.getCurrentHp();

        if (enemyHpDelta < 0 && myHpDelta >= 0) {
            return AttackAnimationDirection.PLAYER;
        }
        if (myHpDelta < 0 && enemyHpDelta >= 0) {
            return AttackAnimationDirection.ENEMY;
        }
        if (myHpDelta < 0 && enemyHpDelta < 0) {
            return Math.abs(enemyHpDelta) >= Math.abs(myHpDelta)
                ? AttackAnimationDirection.PLAYER
                : AttackAnimationDirection.ENEMY;
        }

        return AttackAnimationDirection.NONE;
    }
    
    private ImageIcon loadPokemonSprite(int id, boolean isEnemy) {
        String dir = isEnemy ? FRONT_IMAGE_DIR : BACK_IMAGE_DIR;
        String file = dir + id + ".gif";
        File f = new File(file);
        
        if (f.exists()) {
            ImageIcon icon = new ImageIcon(f.getPath());
            Image img = icon.getImage().getScaledInstance(200, 200, Image.SCALE_DEFAULT);
            return new ImageIcon(img);
        }
        return createPlaceholderSprite();
    }
    
    private ImageIcon loadPokemonSmallIcon(int id) {
        String file = ICON_IMAGE_DIR + id + ".png";
        File f = new File(file);
        
        if (f.exists()) {
            ImageIcon icon = new ImageIcon(file);
            Image img = icon.getImage().getScaledInstance(55, 55, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        }
        return createPlaceholderIcon();
    }
    
    private ImageIcon createPlaceholderSprite() {
        java.awt.image.BufferedImage placeholder = new java.awt.image.BufferedImage(200, 200, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = placeholder.createGraphics();
        g2d.setColor(new Color(100, 100, 100));
        g2d.fillOval(40, 40, 120, 120);
        g2d.dispose();
        return new ImageIcon(placeholder);
    }
    
    private ImageIcon createPlaceholderIcon() {
        java.awt.image.BufferedImage placeholder = new java.awt.image.BufferedImage(55, 55, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = placeholder.createGraphics();
        g2d.setColor(Color.GRAY);
        g2d.fillOval(8, 8, 40, 40);
        g2d.dispose();
        return new ImageIcon(placeholder);
    }
    
    // BattleClientListener implementation
    
    @Override
    public void onGameCreated(String gameId) {
        // Not used in battle panel
    }
    
    @Override
    public void onGameJoined(String gameId, String opponentName) {
        SwingUtilities.invokeLater(() -> {
            this.opponentUsername = opponentName;
            updateTeamTitleLabels();
        });
    }
    
    @Override
    public void onBattleStarted(BattleStateDTO initialState) {
        SwingUtilities.invokeLater(() -> {
            this.currentState = initialState;
            updateBattleState(initialState);
        });
    }
    
    @Override
    public void onBattleStateUpdate(BattleStateDTO state, String actionMessage) {
        SwingUtilities.invokeLater(() -> {
            AttackAnimationDirection animationDirection = determineAttackAnimation(state);
            Runnable applyStateUpdate = () -> {
                isProcessing = false;
                updateBattleState(state);
            };

            if (actionMessage != null && !actionMessage.isEmpty()) {
                isProcessing = true;
                showBattleMessage(actionMessage, 1500, () -> {
                    if (animationDirection != AttackAnimationDirection.NONE) {
                        animateAttack(animationDirection == AttackAnimationDirection.PLAYER, applyStateUpdate);
                    } else {
                        applyStateUpdate.run();
                    }
                });
            } else if (animationDirection != AttackAnimationDirection.NONE) {
                isProcessing = true;
                animateAttack(animationDirection == AttackAnimationDirection.PLAYER, applyStateUpdate);
            } else {
                applyStateUpdate.run();
            }
        });
    }
    
    @Override
    public void onTurnComplete() {
        SwingUtilities.invokeLater(() -> {
            if (currentState != null && !isProcessing) {
                checkTurnAndEnableControls();
            }
        });
    }
    
    @Override
    public void onBattleEnd(boolean didIWin,
                            String winnerName,
                            String loserName,
                            BattleOutcomeType outcomeType) {
        SwingUtilities.invokeLater(() ->
            showBattleOutcomeDialog(didIWin, winnerName, loserName, outcomeType)
        );
    }
    
    @Override
    public void onError(String errorCode, String errorMessage) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                errorMessage,
                I18n.get("common.error"),
                JOptionPane.ERROR_MESSAGE);
        });
    }
    
    @Override
    public void onGameError(String error) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                error,
                I18n.get("common.error"),
                JOptionPane.ERROR_MESSAGE);
        });
    }
    
    @Override
    public void onConnectionLost() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                I18n.get("multiplayer.battle.connectionLost"),
                I18n.get("common.error"),
                JOptionPane.ERROR_MESSAGE);
            returnToPokedex();
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        GradientPaint gp = new GradientPaint(
            0, 0, new Color(45, 52, 90),
            0, getHeight(), new Color(88, 103, 168)
        );
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, getWidth(), getHeight());
    }
}