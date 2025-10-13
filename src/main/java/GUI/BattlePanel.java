package GUI;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.json.JSONArray;
import org.json.JSONObject;

import database.DatabaseConnection;
import model.Pokemon;

public class BattlePanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(BattlePanel.class.getName());
    private static final String BACK_IMAGE_DIR = "Images/Back-Pokemon-gif/";
    private static final String FRONT_IMAGE_DIR = "Images/Front-Pokemon-gif/";
    
    private final Pokemon playerPokemon;
    private final Pokemon enemyPokemon;
    private final Connection pokedexConn;
    private final Connection usuariosConn;
    private final String username;
    
    private int playerCurrentHP;
    private int enemyCurrentHP;
    private final int playerMaxHP;
    private final int enemyMaxHP;
    
    private JLabel playerHPLabel, enemyHPLabel;
    private JProgressBar playerHealthBar, enemyHealthBar;
    private JTextArea battleLog;
    private JButton attack1Button, attack2Button, attack3Button, attack4Button;
    private JButton backButton;
    private JLabel playerSpriteLabel, enemySpriteLabel;
    private JPanel battleMessagePanel;
    private JLabel battleMessageLabel;
    
    private boolean playerTurn = true;
    private boolean battleEnded = false;
    private javax.swing.Timer animationTimer;
    
    private Attack[] playerAttacks;
    private Attack[] enemyAttacks;
    
    private JSONObject movesData;
    private JSONObject movesPokemon;
    
    public BattlePanel(Pokemon player, Pokemon enemy, Connection pokedexConn, Connection usuariosConn, String username) {
        this.playerPokemon = player;
        this.enemyPokemon = enemy;
        this.pokedexConn = pokedexConn;
        this.usuariosConn = usuariosConn;
        this.username = username;
        
        this.playerMaxHP = player.getHp();
        this.enemyMaxHP = enemy.getHp();
        this.playerCurrentHP = playerMaxHP;
        this.enemyCurrentHP = enemyMaxHP;
        
        // Load JSON data
        loadJSONData();
        
        // Generate attacks from JSON
        this.playerAttacks = generateAttacksFromJSON(player);
        this.enemyAttacks = generateAttacksFromJSON(enemy);
        
        // Determine who goes first based on Speed
        playerTurn = player.getSpeed() >= enemy.getSpeed();
        
        initializeUI();
        
        // Start battle sequence
        SwingUtilities.invokeLater(() -> {
            showBattleMessage("Um " + enemy.getName() + " selvagem apareceu!", 2000, () -> {
                showBattleMessage("Vamos lá, " + player.getName() + "!", 2000, () -> {
                    if (!playerTurn) {
                        showBattleMessage(enemy.getName() + " é mais rápido!\nEle irá atacar primeiro!", 2500, () -> {
                            executeEnemyTurn();
                        });
                    } else {
                        showBattleMessage("O que " + player.getName() + " deve fazer?", 1500, () -> {
                            enableAttackButtons();
                        });
                    }
                });
            });
        });
    }
    
    private void loadJSONData() {
        try {
            String movesDataContent = new String(Files.readAllBytes(Paths.get("movesData.json")));
            movesData = new JSONObject(movesDataContent);
            
            String movesPokemonContent = new String(Files.readAllBytes(Paths.get("movesPokemon.json")));
            movesPokemon = new JSONObject(movesPokemonContent);
            
            LOGGER.info("JSON data loaded successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading JSON data", e);
            movesData = new JSONObject();
            movesPokemon = new JSONObject();
        }
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        setBackground(new Color(240, 245, 250));
        
        add(createBattleFieldPanel(), BorderLayout.CENTER);
        add(createControlPanel(), BorderLayout.SOUTH);
    }
    
    private JPanel createBattleFieldPanel() {
        JPanel battleField = new JPanel(new BorderLayout(20, 20));
        battleField.setOpaque(false);
        
        // Top - Enemy Pokemon
        JPanel enemyPanel = createEnemyPanel();
        battleField.add(enemyPanel, BorderLayout.NORTH);
        
        // Center - Battle Message Area
        battleMessagePanel = new JPanel(new BorderLayout());
        battleMessagePanel.setOpaque(true);
        battleMessagePanel.setBackground(Color.WHITE);
        battleMessagePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 150, 200), 4, true),
            BorderFactory.createEmptyBorder(30, 30, 30, 30)
        ));
        battleMessagePanel.setPreferredSize(new Dimension(800, 200));
        
        battleMessageLabel = new JLabel("", SwingConstants.CENTER);
        battleMessageLabel.setFont(new Font("Arial", Font.BOLD, 22));
        battleMessageLabel.setForeground(new Color(50, 50, 50));
        battleMessagePanel.add(battleMessageLabel, BorderLayout.CENTER);
        
        battleField.add(battleMessagePanel, BorderLayout.CENTER);
        
        // Bottom - Player Pokemon
        JPanel playerPanel = createPlayerPanel();
        battleField.add(playerPanel, BorderLayout.SOUTH);
        
        return battleField;
    }
    
    private JPanel createEnemyPanel() {
        JPanel panel = new JPanel(new BorderLayout(25, 20));
        panel.setOpaque(false);
        
        // Left side - Enemy info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(true);
        infoPanel.setBackground(new Color(255, 240, 240));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 20, 60), 4, true),
            BorderFactory.createEmptyBorder(20, 25, 20, 25)
        ));
        
        JLabel nameLabel = new JLabel("⚔ FOE " + enemyPokemon.getName().toUpperCase());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 26));
        nameLabel.setForeground(new Color(220, 20, 60));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(nameLabel);
        
        infoPanel.add(Box.createVerticalStrut(15));
        
        JPanel levelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        levelPanel.setOpaque(false);
        JLabel levelLabel = new JLabel(" Lv. 50 ");
        levelLabel.setFont(new Font("Arial", Font.BOLD, 18));
        levelLabel.setForeground(Color.WHITE);
        levelLabel.setOpaque(true);
        levelLabel.setBackground(new Color(220, 20, 60));
        levelLabel.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        levelPanel.add(levelLabel);
        infoPanel.add(levelPanel);
        
        infoPanel.add(Box.createVerticalStrut(12));
        
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        typePanel.setOpaque(false);
        typePanel.add(createTypeBadge(enemyPokemon.getType1()));
        if (enemyPokemon.getType2() != null && !enemyPokemon.getType2().isEmpty() && !enemyPokemon.getType2().equals("None")) {
            typePanel.add(createTypeBadge(enemyPokemon.getType2()));
        }
        infoPanel.add(typePanel);
        
        infoPanel.add(Box.createVerticalStrut(15));
        
        JPanel statsPanel = new JPanel(new GridLayout(3, 2, 12, 8));
        statsPanel.setOpaque(false);
        statsPanel.add(createStatLabel("ATK", enemyPokemon.getAttack()));
        statsPanel.add(createStatLabel("DEF", enemyPokemon.getDefense()));
        statsPanel.add(createStatLabel("SP.ATK", enemyPokemon.getSpAtk()));
        statsPanel.add(createStatLabel("SP.DEF", enemyPokemon.getSpDef()));
        statsPanel.add(createStatLabel("SPEED", enemyPokemon.getSpeed()));
        statsPanel.add(new JLabel(""));
        infoPanel.add(statsPanel);
        
        infoPanel.add(Box.createVerticalStrut(20));
        
        enemyHPLabel = new JLabel("HP: " + enemyCurrentHP + " / " + enemyMaxHP);
        enemyHPLabel.setFont(new Font("Arial", Font.BOLD, 20));
        enemyHPLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(enemyHPLabel);
        
        infoPanel.add(Box.createVerticalStrut(10));
        
        enemyHealthBar = new JProgressBar(0, enemyMaxHP);
        enemyHealthBar.setValue(enemyCurrentHP);
        enemyHealthBar.setStringPainted(true);
        enemyHealthBar.setString("100,0%");
        enemyHealthBar.setFont(new Font("Arial", Font.BOLD, 16));
        enemyHealthBar.setForeground(new Color(76, 175, 80));
        enemyHealthBar.setBackground(new Color(220, 220, 220));
        enemyHealthBar.setPreferredSize(new Dimension(450, 40));
        enemyHealthBar.setMaximumSize(new Dimension(450, 40));
        enemyHealthBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        enemyHealthBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY, 3),
            BorderFactory.createEmptyBorder(3, 3, 3, 3)
        ));
        infoPanel.add(enemyHealthBar);
        
        // Right side - Enemy sprite
        JPanel spritePanel = new JPanel(new BorderLayout());
        spritePanel.setOpaque(false);
        spritePanel.setPreferredSize(new Dimension(300, 300));
        
        enemySpriteLabel = new JLabel();
        enemySpriteLabel.setHorizontalAlignment(SwingConstants.CENTER);
        enemySpriteLabel.setVerticalAlignment(SwingConstants.CENTER);
        enemySpriteLabel.setIcon(loadPokemonSprite(enemyPokemon.getId(), true));
        spritePanel.add(enemySpriteLabel, BorderLayout.CENTER);
        
        panel.add(infoPanel, BorderLayout.WEST);
        panel.add(spritePanel, BorderLayout.EAST);
        
        return panel;
    }
    
    private JPanel createPlayerPanel() {
        JPanel panel = new JPanel(new BorderLayout(25, 20));
        panel.setOpaque(false);
        
        // Left side - Player sprite
        JPanel spritePanel = new JPanel(new BorderLayout());
        spritePanel.setOpaque(false);
        spritePanel.setPreferredSize(new Dimension(300, 300));
        
        playerSpriteLabel = new JLabel();
        playerSpriteLabel.setHorizontalAlignment(SwingConstants.CENTER);
        playerSpriteLabel.setVerticalAlignment(SwingConstants.CENTER);
        playerSpriteLabel.setIcon(loadPokemonSprite(playerPokemon.getId(), false));
        spritePanel.add(playerSpriteLabel, BorderLayout.CENTER);
        
        // Right side - Player info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(true);
        infoPanel.setBackground(new Color(240, 255, 240));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(76, 175, 80), 4, true),
            BorderFactory.createEmptyBorder(20, 25, 20, 25)
        ));
        
        JLabel nameLabel = new JLabel("★ " + playerPokemon.getName().toUpperCase());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 26));
        nameLabel.setForeground(new Color(76, 175, 80));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(nameLabel);
        
        infoPanel.add(Box.createVerticalStrut(15));
        
        JPanel levelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        levelPanel.setOpaque(false);
        JLabel levelLabel = new JLabel(" Lv. 50 ");
        levelLabel.setFont(new Font("Arial", Font.BOLD, 18));
        levelLabel.setForeground(Color.WHITE);
        levelLabel.setOpaque(true);
        levelLabel.setBackground(new Color(76, 175, 80));
        levelLabel.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        levelPanel.add(levelLabel);
        infoPanel.add(levelPanel);
        
        infoPanel.add(Box.createVerticalStrut(12));
        
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        typePanel.setOpaque(false);
        typePanel.add(createTypeBadge(playerPokemon.getType1()));
        if (playerPokemon.getType2() != null && !playerPokemon.getType2().isEmpty() && !playerPokemon.getType2().equals("None")) {
            typePanel.add(createTypeBadge(playerPokemon.getType2()));
        }
        infoPanel.add(typePanel);
        
        infoPanel.add(Box.createVerticalStrut(15));
        
        JPanel statsPanel = new JPanel(new GridLayout(3, 2, 12, 8));
        statsPanel.setOpaque(false);
        statsPanel.add(createStatLabel("ATK", playerPokemon.getAttack()));
        statsPanel.add(createStatLabel("DEF", playerPokemon.getDefense()));
        statsPanel.add(createStatLabel("SP.ATK", playerPokemon.getSpAtk()));
        statsPanel.add(createStatLabel("SP.DEF", playerPokemon.getSpDef()));
        statsPanel.add(createStatLabel("SPEED", playerPokemon.getSpeed()));
        statsPanel.add(new JLabel(""));
        infoPanel.add(statsPanel);
        
        infoPanel.add(Box.createVerticalStrut(20));
        
        playerHPLabel = new JLabel("HP: " + playerCurrentHP + " / " + playerMaxHP);
        playerHPLabel.setFont(new Font("Arial", Font.BOLD, 20));
        playerHPLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(playerHPLabel);
        
        infoPanel.add(Box.createVerticalStrut(10));
        
        playerHealthBar = new JProgressBar(0, playerMaxHP);
        playerHealthBar.setValue(playerCurrentHP);
        playerHealthBar.setStringPainted(true);
        playerHealthBar.setString("100,0%");
        playerHealthBar.setFont(new Font("Arial", Font.BOLD, 16));
        playerHealthBar.setForeground(new Color(76, 175, 80));
        playerHealthBar.setBackground(new Color(220, 220, 220));
        playerHealthBar.setPreferredSize(new Dimension(450, 40));
        playerHealthBar.setMaximumSize(new Dimension(450, 40));
        playerHealthBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        playerHealthBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY, 3),
            BorderFactory.createEmptyBorder(3, 3, 3, 3)
        ));
        infoPanel.add(playerHealthBar);
        
        panel.add(spritePanel, BorderLayout.WEST);
        panel.add(infoPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    private void showBattleMessage(String message, int duration, Runnable onComplete) {
        battleMessageLabel.setText("<html><center>" + message.replace("\n", "<br>") + "</center></html>");
        javax.swing.Timer timer = new javax.swing.Timer(duration, e -> {
            if (onComplete != null) {
                onComplete.run();
            }
        });
        timer.setRepeats(false);
        timer.start();
        timer.setRepeats(false);
        timer.start();
    }
    
    private JLabel createTypeBadge(String type) {
        JLabel badge = new JLabel(" " + getTypeEmoji(type) + " " + type.toUpperCase() + " ");
        badge.setFont(new Font("Arial", Font.BOLD, 13));
        badge.setForeground(Color.WHITE);
        badge.setOpaque(true);
        badge.setBackground(getTypeColor(type));
        badge.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getTypeColor(type).darker(), 2),
            BorderFactory.createEmptyBorder(3, 10, 3, 10)
        ));
        return badge;
    }
    
    private JLabel createStatLabel(String name, int value) {
        JLabel label = new JLabel(name + ": " + value);
        label.setFont(new Font("Arial", Font.PLAIN, 13));
        label.setForeground(new Color(70, 70, 70));
        return label;
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
    
    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout(15, 15));
        controlPanel.setOpaque(false);
        
        JPanel attackPanel = new JPanel(new GridLayout(2, 2, 18, 18));
        attackPanel.setOpaque(false);
        attackPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIUtils.PRIMARY_COLOR, 4, true), 
                "⚔ CHOOSE YOUR ATTACK", 
                0, 0, 
                new Font("Arial", Font.BOLD, 20), 
                UIUtils.PRIMARY_COLOR
            ),
            BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));
        
        attack1Button = createAttackButton(playerAttacks[0]);
        attack2Button = createAttackButton(playerAttacks[1]);
        attack3Button = createAttackButton(playerAttacks[2]);
        attack4Button = createAttackButton(playerAttacks[3]);
        
        attackPanel.add(attack1Button);
        attackPanel.add(attack2Button);
        attackPanel.add(attack3Button);
        attackPanel.add(attack4Button);
        
        controlPanel.add(attackPanel, BorderLayout.CENTER);
        
        JPanel bottomButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 12));
        bottomButtons.setOpaque(false);
        
        backButton = UIUtils.createStyledButton("« Return to Pokédex", e -> returnToPokedex(), "Voltar ao Pokédex");
        backButton.setPreferredSize(new Dimension(220, 45));
        bottomButtons.add(backButton);
        
        controlPanel.add(bottomButtons, BorderLayout.SOUTH);
        
        disableAttackButtons();
        
        return controlPanel;
    }
    
    private JButton createAttackButton(Attack attack) {
        String typeEmoji = getTypeEmoji(attack.type);
        Color typeColor = getTypeColor(attack.type);
        
        JButton button = new JButton(String.format(
            "<html><center><b style='font-size:15px'>%s %s</b><br>" +
            "<span style='font-size:12px'>Type: %s | Power: %d | PP: ∞</span></center></html>", 
            typeEmoji, attack.name, attack.type.substring(0,1).toUpperCase() + attack.type.substring(1), attack.power));
        
        button.setFont(new Font("Arial", Font.PLAIN, 13));
        button.setBackground(typeColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(typeColor.darker(), 3, true),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        button.setPreferredSize(new Dimension(300, 85));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        
        button.addActionListener(e -> executePlayerAttack(attack));
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(typeColor.brighter());
                    button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.WHITE, 3, true),
                        BorderFactory.createEmptyBorder(12, 12, 12, 12)
                    ));
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(typeColor);
                    button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(typeColor.darker(), 3, true),
                        BorderFactory.createEmptyBorder(12, 12, 12, 12)
                    ));
                }
            }
        });
        
        return button;
    }
    
    private void executePlayerAttack(Attack attack) {
        if (battleEnded || !playerTurn) {
            return;
        }
        
        disableAttackButtons();
        
        showBattleMessage(playerPokemon.getName() + " usou " + attack.name + "!", 1500, () -> {
            double effectiveness = getTypeEffectiveness(attack.type, enemyPokemon.getType1(), enemyPokemon.getType2());
            int damage = calculateDamage(playerPokemon.getAttack(), playerPokemon.getSpAtk(), 
                                        enemyPokemon.getDefense(), enemyPokemon.getSpDef(), 
                                        attack.power, attack.category, effectiveness);
            
            animateAttack(true, () -> {
                enemyCurrentHP = Math.max(0, enemyCurrentHP - damage);
                updateHealthBar(false);
                
                String effectMsg = "";
                if (effectiveness > 1.5) {
                    effectMsg = "\n\n★★ É SUPER EFETIVO! ★★";
                } else if (effectiveness < 0.75 && effectiveness > 0) {
                    effectMsg = "\n\n☆ Não é muito efetivo...";
                } else if (effectiveness == 0) {
                    effectMsg = "\n\n✕ Não afeta o inimigo...";
                }
                
                showBattleMessage("Causou " + damage + " de dano!" + effectMsg, 2000, () -> {
                    if (enemyCurrentHP <= 0) {
                        endBattle(true);
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
        
        Random rand = new Random();
        Attack attack = enemyAttacks[rand.nextInt(4)];
        
        showBattleMessage("Inimigo " + enemyPokemon.getName() + " usou " + attack.name + "!", 1500, () -> {
            double effectiveness = getTypeEffectiveness(attack.type, playerPokemon.getType1(), playerPokemon.getType2());
            int damage = calculateDamage(enemyPokemon.getAttack(), enemyPokemon.getSpAtk(),
                                        playerPokemon.getDefense(), playerPokemon.getSpDef(),
                                        attack.power, attack.category, effectiveness);
            
            animateAttack(false, () -> {
                playerCurrentHP = Math.max(0, playerCurrentHP - damage);
                updateHealthBar(true);
                
                String effectMsg = "";
                if (effectiveness > 1.5) {
                    effectMsg = "\n\n★★ É SUPER EFETIVO! ★★";
                } else if (effectiveness < 0.75 && effectiveness > 0) {
                    effectMsg = "\n\n☆ Não é muito efetivo...";
                } else if (effectiveness == 0) {
                    effectMsg = "\n\n✕ Não te afeta...";
                }
                
                showBattleMessage("Causou " + damage + " de dano!" + effectMsg, 2000, () -> {
                    if (playerCurrentHP <= 0) {
                        endBattle(false);
                    } else {
                        playerTurn = true;
                        showBattleMessage("O que " + playerPokemon.getName() + " deve fazer?", 1000, () -> {
                            enableAttackButtons();
                        });
                    }
                });
            });
        });
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
    
    private void animateAttack(boolean isPlayerAttacking, Runnable onComplete) {
        final int[] frame = {0};
        final int maxFrames = 15;
        
        JLabel attackingSprite = isPlayerAttacking ? playerSpriteLabel : enemySpriteLabel;
        JLabel defendingSprite = isPlayerAttacking ? enemySpriteLabel : playerSpriteLabel;
        
        final Point originalPosAtk = attackingSprite.getLocation();
        final Point originalPosDef = defendingSprite.getLocation();
        
        animationTimer = new javax.swing.Timer(30, null);
        animationTimer.addActionListener(e -> {
            frame[0]++;
            
            if (frame[0] <= 5) {
                int offset = (frame[0] % 2 == 0) ? 12 : -12;
                attackingSprite.setLocation(originalPosAtk.x + offset, originalPosAtk.y);
            } else if (frame[0] <= 10) {
                attackingSprite.setLocation(originalPosAtk);
                if (frame[0] % 2 == 0) {
                    defendingSprite.setVisible(false);
                } else {
                    defendingSprite.setVisible(true);
                }
                int offset = (frame[0] % 2 == 0) ? 8 : -8;
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
    
    private void updateHealthBar(boolean isPlayer) {
        if (isPlayer) {
            playerHealthBar.setValue(playerCurrentHP);
            double percentage = ((double) playerCurrentHP / playerMaxHP) * 100;
            playerHealthBar.setString(String.format("%.1f%%", percentage));
            playerHPLabel.setText("HP: " + playerCurrentHP + " / " + playerMaxHP);
            
            if (playerCurrentHP < playerMaxHP * 0.2) {
                playerHealthBar.setForeground(new Color(220, 20, 60));
            } else if (playerCurrentHP < playerMaxHP * 0.5) {
                playerHealthBar.setForeground(new Color(255, 165, 0));
            } else {
                playerHealthBar.setForeground(new Color(76, 175, 80));
            }
        } else {
            enemyHealthBar.setValue(enemyCurrentHP);
            double percentage = ((double) enemyCurrentHP / enemyMaxHP) * 100;
            enemyHealthBar.setString(String.format("%.1f%%", percentage));
            enemyHPLabel.setText("HP: " + enemyCurrentHP + " / " + enemyMaxHP);
            
            if (enemyCurrentHP < enemyMaxHP * 0.2) {
                enemyHealthBar.setForeground(new Color(220, 20, 60));
            } else if (enemyCurrentHP < enemyMaxHP * 0.5) {
                enemyHealthBar.setForeground(new Color(255, 165, 0));
            } else {
                enemyHealthBar.setForeground(new Color(76, 175, 80));
            }
        }
    }
    
    private void endBattle(boolean playerWon) {
        battleEnded = true;
        disableAttackButtons();
        
        if (playerWon) {
            showBattleMessage("Inimigo " + enemyPokemon.getName() + " desmaiou!\n\n★★★ VITÓRIA! ★★★\n" + 
                            playerPokemon.getName() + " ganhou experiência!", 3000, () -> {
                JOptionPane.showMessageDialog(this, 
                    "🎉 PARABÉNS! 🎉\n\n" + 
                    playerPokemon.getName() + " derrotou " + enemyPokemon.getName() + "!\n\n" +
                    "Você é vitorioso!",
                    "⭐ VITÓRIA ⭐", JOptionPane.INFORMATION_MESSAGE);
            });
        } else {
            showBattleMessage(playerPokemon.getName() + " desmaiou!\n\n✕✕✕ DERROTA ✕✕✕\n" +
                            "Você correu para o Centro Pokémon...", 3000, () -> {
                JOptionPane.showMessageDialog(this, 
                    "💔 Oh não! 💔\n\n" + 
                    playerPokemon.getName() + " foi derrotado por " + enemyPokemon.getName() + "!\n\n" +
                    "Não desista, Treinador!",
                    "Derrota", JOptionPane.WARNING_MESSAGE);
            });
        }
    }
    
    private void disableAttackButtons() {
        attack1Button.setEnabled(false);
        attack2Button.setEnabled(false);
        attack3Button.setEnabled(false);
        attack4Button.setEnabled(false);
    }
    
    private void enableAttackButtons() {
        if (!battleEnded && playerTurn) {
            attack1Button.setEnabled(true);
            attack2Button.setEnabled(true);
            attack3Button.setEnabled(true);
            attack4Button.setEnabled(true);
        }
    }
    
    private ImageIcon loadPokemonSprite(int id, boolean isEnemy) {
        String dir = isEnemy ? FRONT_IMAGE_DIR : BACK_IMAGE_DIR;
        String file = dir + id + ".gif";
        File f = new File(file);
        
        if (!f.exists()) {
            LOGGER.log(Level.WARNING, "Sprite not found: " + file);
            return createPlaceholderSprite(isEnemy);
        }
        
        ImageIcon icon = new ImageIcon(file);
        Image img = icon.getImage().getScaledInstance(280, 280, Image.SCALE_DEFAULT);
        return new ImageIcon(img);
    }
    
    private ImageIcon createPlaceholderSprite(boolean isEnemy) {
        BufferedImage placeholder = new BufferedImage(280, 280, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = placeholder.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        GradientPaint gp = new GradientPaint(0, 0, new Color(200, 200, 200), 
                                             280, 280, new Color(150, 150, 150));
        g2d.setPaint(gp);
        g2d.fillOval(60, 60, 160, 160);
        
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(4));
        g2d.drawOval(60, 60, 160, 160);
        
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        String text = isEnemy ? "ENEMY" : "PLAYER";
        FontMetrics fm = g2d.getFontMetrics();
        int x = (280 - fm.stringWidth(text)) / 2;
        g2d.drawString(text, x, 145);
        
        g2d.dispose();
        return new ImageIcon(placeholder);
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
                
                // Fill remaining slots if less than 4 moves
                for (int i = moves.length(); i < 4; i++) {
                    attacks[i] = new Attack("Tackle", "normal", 40, "physical");
                }
                
            } else {
                // Fallback to type-based attacks
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
        Map<String, Attack[]> typeAttacks = getTypeAttacks();
        Attack[] attacks = new Attack[4];
        
        Attack[] type1Attacks = typeAttacks.getOrDefault(type1, typeAttacks.get("Normal"));
        attacks[0] = type1Attacks[0];
        attacks[1] = type1Attacks[1];
        attacks[2] = type1Attacks[2];
        attacks[3] = new Attack("Tackle", "normal", 40, "physical");
        
        return attacks;
    }
    
    private Map<String, Attack[]> getTypeAttacks() {
        Map<String, Attack[]> attacks = new HashMap<>();
        
        attacks.put("Normal", new Attack[]{
            new Attack("Tackle", "normal", 40, "physical"),
            new Attack("Body Slam", "normal", 85, "physical"),
            new Attack("Hyper Beam", "normal", 150, "special")
        });
        
        attacks.put("Fire", new Attack[]{
            new Attack("Ember", "fire", 40, "special"),
            new Attack("Flamethrower", "fire", 90, "special"),
            new Attack("Fire Blast", "fire", 110, "special")
        });
        
        attacks.put("Water", new Attack[]{
            new Attack("Water Gun", "water", 40, "special"),
            new Attack("Surf", "water", 95, "special"),
            new Attack("Hydro Pump", "water", 110, "special")
        });
        
        attacks.put("Electric", new Attack[]{
            new Attack("Thunder Shock", "electric", 40, "special"),
            new Attack("Thunderbolt", "electric", 90, "special"),
            new Attack("Thunder", "electric", 110, "special")
        });
        
        attacks.put("Grass", new Attack[]{
            new Attack("Vine Whip", "grass", 45, "physical"),
            new Attack("Razor Leaf", "grass", 55, "physical"),
            new Attack("Solar Beam", "grass", 120, "special")
        });
        
        attacks.put("Ice", new Attack[]{
            new Attack("Ice Beam", "ice", 90, "special"),
            new Attack("Blizzard", "ice", 110, "special"),
            new Attack("Ice Punch", "ice", 75, "physical")
        });
        
        attacks.put("Fighting", new Attack[]{
            new Attack("Karate Chop", "fighting", 50, "physical"),
            new Attack("Low Kick", "fighting", 65, "physical"),
            new Attack("High Jump Kick", "fighting", 130, "physical")
        });
        
        attacks.put("Poison", new Attack[]{
            new Attack("Poison Sting", "poison", 15, "physical"),
            new Attack("Sludge Bomb", "poison", 90, "special"),
            new Attack("Gunk Shot", "poison", 120, "physical")
        });
        
        attacks.put("Ground", new Attack[]{
            new Attack("Dig", "ground", 80, "physical"),
            new Attack("Earthquake", "ground", 100, "physical"),
            new Attack("Earth Power", "ground", 90, "special")
        });
        
        attacks.put("Flying", new Attack[]{
            new Attack("Wing Attack", "flying", 60, "physical"),
            new Attack("Drill Peck", "flying", 80, "physical"),
            new Attack("Sky Attack", "flying", 140, "physical")
        });
        
        attacks.put("Psychic", new Attack[]{
            new Attack("Confusion", "psychic", 50, "special"),
            new Attack("Psychic", "psychic", 90, "special"),
            new Attack("Psystrike", "psychic", 100, "special")
        });
        
        attacks.put("Bug", new Attack[]{
            new Attack("Twineedle", "bug", 25, "physical"),
            new Attack("X-Scissor", "bug", 80, "physical"),
            new Attack("Megahorn", "bug", 120, "physical")
        });
        
        attacks.put("Rock", new Attack[]{
            new Attack("Rock Throw", "rock", 50, "physical"),
            new Attack("Rock Slide", "rock", 75, "physical"),
            new Attack("Stone Edge", "rock", 100, "physical")
        });
        
        attacks.put("Ghost", new Attack[]{
            new Attack("Lick", "ghost", 30, "physical"),
            new Attack("Shadow Ball", "ghost", 80, "special"),
            new Attack("Shadow Claw", "ghost", 70, "physical")
        });
        
        attacks.put("Dragon", new Attack[]{
            new Attack("Dragon Rage", "dragon", 90, "special"),
            new Attack("Dragon Pulse", "dragon", 85, "special"),
            new Attack("Outrage", "dragon", 120, "physical")
        });
        
        attacks.put("Dark", new Attack[]{
            new Attack("Bite", "dark", 60, "physical"),
            new Attack("Crunch", "dark", 80, "physical"),
            new Attack("Dark Pulse", "dark", 80, "special")
        });
        
        attacks.put("Steel", new Attack[]{
            new Attack("Iron Tail", "steel", 100, "physical"),
            new Attack("Metal Claw", "steel", 50, "physical"),
            new Attack("Flash Cannon", "steel", 80, "special")
        });
        
        attacks.put("Fairy", new Attack[]{
            new Attack("Fairy Wind", "fairy", 40, "special"),
            new Attack("Moonblast", "fairy", 95, "special"),
            new Attack("Dazzling Gleam", "fairy", 80, "special")
        });
        
        return attacks;
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
    
    private String getTypeEmoji(String type) {
        return switch (type.toLowerCase()) {
            case "fire" -> "🔥";
            case "water" -> "💧";
            case "electric" -> "⚡";
            case "grass" -> "🍃";
            case "ice" -> "❄️";
            case "fighting" -> "👊";
            case "poison" -> "☠️";
            case "ground" -> "⛰️";
            case "flying" -> "🦅";
            case "psychic" -> "🔮";
            case "bug" -> "🐛";
            case "rock" -> "🪨";
            case "ghost" -> "👻";
            case "dragon" -> "🐉";
            case "dark" -> "🌑";
            case "steel" -> "⚙️";
            case "fairy" -> "✨";
            default -> "⭐";
        };
    }
    
    private void returnToPokedex() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        parentFrame.dispose();
        
        SwingUtilities.invokeLater(() -> {
            try {
                Connection newPokedexConn = pokedexConn != null && !pokedexConn.isClosed() ?
                    pokedexConn : DatabaseConnection.connect("pokedex.db");
                Connection newUsersConn = usuariosConn != null && !usuariosConn.isClosed() ?
                    usuariosConn : DatabaseConnection.connect("Usuarios.db");
                JFrame frame = new JFrame("Pokédex");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(1200, 900);
                frame.setLocationRelativeTo(null);
                frame.setContentPane(new PokedexPanel(newPokedexConn, newUsersConn, frame, username));
                frame.setVisible(true);
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Erro ao conectar ao banco de dados", ex);
                JOptionPane.showMessageDialog(null, 
                    "Erro ao retornar ao Pokédex: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        GradientPaint gp = new GradientPaint(0, 0, new Color(135, 206, 250), 
                                             0, getHeight(), new Color(176, 224, 230));
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, getWidth(), getHeight());
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