package GUI;

import database.DatabaseConnection;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import model.Pokemon;

public class BattlePanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(BattlePanel.class.getName());
    private static final String BACK_IMAGE_DIR = "Images/Back-Pokemon/";
    private static final String FRONT_IMAGE_DIR = "Images/Front-Pokemon/";
    
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
    private JLabel playerNameLabel, enemyNameLabel;
    private JPanel playerInfoPanel, enemyInfoPanel;
    
    private boolean playerTurn = true;
    private boolean battleEnded = false;
    private Timer animationTimer;
    
    private Attack[] playerAttacks;
    private Attack[] enemyAttacks;
    
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
        
        // Determine who goes first based on Speed
        playerTurn = player.getSpeed() >= enemy.getSpeed();
        
        // Generate attacks based on Pokemon types
        this.playerAttacks = generateAttacks(player);
        this.enemyAttacks = generateAttacks(enemy);
        
        initializeUI();
        logMessage("═══════════════════════════════════════");
        logMessage("A wild " + enemy.getName() + " appeared!");
        logMessage("Go! " + player.getName() + "!");
        logMessage("═══════════════════════════════════════");
        if (!playerTurn) {
            logMessage(enemy.getName() + " is faster! Enemy attacks first!");
            SwingUtilities.invokeLater(() -> {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                executeEnemyTurn();
            });
        }
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(new Color(240, 240, 240));
        
        add(createBattleFieldPanel(), BorderLayout.CENTER);
        add(createControlPanel(), BorderLayout.SOUTH);
    }
    
    private JPanel createBattleFieldPanel() {
        JPanel battleField = new JPanel(new BorderLayout(10, 10));
        battleField.setOpaque(false);
        
        // Top panel - Enemy Pokemon
        enemyInfoPanel = createPokemonPanel(enemyPokemon, true);
        battleField.add(enemyInfoPanel, BorderLayout.NORTH);
        
        // Center - Battle Log
        battleLog = new JTextArea(10, 40);
        battleLog.setEditable(false);
        battleLog.setFont(new Font("Consolas", Font.PLAIN, 13));
        battleLog.setBackground(new Color(250, 250, 250));
        battleLog.setForeground(new Color(33, 33, 33));
        battleLog.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIUtils.PRIMARY_COLOR, 2, true),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        JScrollPane logScroll = new JScrollPane(battleLog);
        battleField.add(logScroll, BorderLayout.CENTER);
        
        // Bottom panel - Player Pokemon
        playerInfoPanel = createPokemonPanel(playerPokemon, false);
        battleField.add(playerInfoPanel, BorderLayout.SOUTH);
        
        return battleField;
    }
    
    private JPanel createPokemonPanel(Pokemon pokemon, boolean isEnemy) {
        JPanel panel = new JPanel(new BorderLayout(15, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(isEnemy ? new Color(220, 20, 60) : new Color(76, 175, 80), 3, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        // Pokemon sprite
        JLabel spriteLabel = new JLabel();
        spriteLabel.setHorizontalAlignment(SwingConstants.CENTER);
        spriteLabel.setIcon(loadPokemonSprite(pokemon.getId(), isEnemy));
        if (isEnemy) {
            enemySpriteLabel = spriteLabel;
        } else {
            playerSpriteLabel = spriteLabel;
        }
        
        // Info panel
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        
        JLabel nameLabel = UIUtils.createLabel((isEnemy ? "⚔ Foe " : "★ ") + pokemon.getName());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 20));
        nameLabel.setForeground(isEnemy ? new Color(220, 20, 60) : new Color(76, 175, 80));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (isEnemy) {
            enemyNameLabel = nameLabel;
        } else {
            playerNameLabel = nameLabel;
        }
        infoPanel.add(nameLabel);
        
        infoPanel.add(Box.createVerticalStrut(8));
        
        JLabel levelLabel = UIUtils.createLabel("Lv. 50");
        levelLabel.setFont(new Font("Arial", Font.BOLD, 14));
        levelLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(levelLabel);
        
        infoPanel.add(Box.createVerticalStrut(5));
        
        JLabel typeLabel = UIUtils.createLabel("Type: " + pokemon.getType1() + 
            (pokemon.getType2() != null && !pokemon.getType2().isEmpty() && !pokemon.getType2().equals("None") 
                ? "/" + pokemon.getType2() : ""));
        typeLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        typeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(typeLabel);
        
        infoPanel.add(Box.createVerticalStrut(8));
        
        JLabel statsLabel = UIUtils.createLabel(String.format("ATK: %d | DEF: %d | SPD: %d", 
            pokemon.getAttack(), pokemon.getDefense(), pokemon.getSpeed()));
        statsLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        statsLabel.setForeground(Color.DARK_GRAY);
        statsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(statsLabel);
        
        infoPanel.add(Box.createVerticalStrut(10));
        
        JLabel hpLabel = UIUtils.createLabel("HP: " + (isEnemy ? enemyCurrentHP : playerCurrentHP) + 
            "/" + (isEnemy ? enemyMaxHP : playerMaxHP));
        hpLabel.setFont(new Font("Arial", Font.BOLD, 14));
        hpLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (isEnemy) {
            enemyHPLabel = hpLabel;
        } else {
            playerHPLabel = hpLabel;
        }
        infoPanel.add(hpLabel);
        
        infoPanel.add(Box.createVerticalStrut(5));
        
        JProgressBar healthBar = new JProgressBar(0, isEnemy ? enemyMaxHP : playerMaxHP);
        healthBar.setValue(isEnemy ? enemyCurrentHP : playerCurrentHP);
        healthBar.setStringPainted(true);
        healthBar.setString(String.format("%.0f%%", ((double)(isEnemy ? enemyCurrentHP : playerCurrentHP) / 
            (isEnemy ? enemyMaxHP : playerMaxHP)) * 100));
        healthBar.setForeground(new Color(76, 175, 80));
        healthBar.setBackground(new Color(220, 220, 220));
        healthBar.setPreferredSize(new Dimension(320, 28));
        healthBar.setMaximumSize(new Dimension(320, 28));
        healthBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        healthBar.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
        if (isEnemy) {
            enemyHealthBar = healthBar;
        } else {
            playerHealthBar = healthBar;
        }
        infoPanel.add(healthBar);
        
        if (isEnemy) {
            panel.add(infoPanel, BorderLayout.WEST);
            panel.add(spriteLabel, BorderLayout.EAST);
        } else {
            panel.add(spriteLabel, BorderLayout.WEST);
            panel.add(infoPanel, BorderLayout.EAST);
        }
        
        return panel;
    }
    
    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout(10, 10));
        controlPanel.setOpaque(false);
        
        JPanel attackPanel = new JPanel(new GridLayout(2, 2, 12, 12));
        attackPanel.setOpaque(false);
        attackPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(UIUtils.PRIMARY_COLOR, 3, true), 
                "⚔ Choose Your Attack", 0, 0, new Font("Arial", Font.BOLD, 16), UIUtils.PRIMARY_COLOR),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        
        attack1Button = createAttackButton(playerAttacks[0], 0);
        attack2Button = createAttackButton(playerAttacks[1], 1);
        attack3Button = createAttackButton(playerAttacks[2], 2);
        attack4Button = createAttackButton(playerAttacks[3], 3);
        
        attackPanel.add(attack1Button);
        attackPanel.add(attack2Button);
        attackPanel.add(attack3Button);
        attackPanel.add(attack4Button);
        
        controlPanel.add(attackPanel, BorderLayout.CENTER);
        
        JPanel bottomButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        bottomButtons.setOpaque(false);
        
        backButton = UIUtils.createStyledButton("« Voltar ao Pokédex", e -> returnToPokedex(), "Voltar ao Pokédex");
        bottomButtons.add(backButton);
        
        controlPanel.add(bottomButtons, BorderLayout.SOUTH);
        
        updateButtonStates();
        
        return controlPanel;
    }
    
    private JButton createAttackButton(Attack attack, int index) {
        String typeEmoji = getTypeEmoji(attack.type);
        JButton button = UIUtils.createStyledButton(
            String.format("<html><center>%s %s<br><small>Type: %s | Power: %d</small></center></html>", 
                typeEmoji, attack.name, attack.type, attack.power), 
            e -> executePlayerAttack(attack), 
            "Use " + attack.name);
        button.setPreferredSize(new Dimension(220, 65));
        return button;
    }
    
    private void executePlayerAttack(Attack attack) {
        if (battleEnded || !playerTurn) {
            return;
        }
        
        disableAttackButtons();
        
        double effectiveness = getTypeEffectiveness(attack.type, enemyPokemon.getType1(), enemyPokemon.getType2());
        int damage = calculateDamage(playerPokemon.getAttack(), enemyPokemon.getDefense(), attack.power, effectiveness);
        
        logMessage("───────────────────────────────────────");
        logMessage("→ " + playerPokemon.getName() + " used " + attack.name + "!");
        
        // Animate attack
        animateAttack(true, () -> {
            enemyCurrentHP = Math.max(0, enemyCurrentHP - damage);
            updateHealthBar(false);
            
            logMessage("  ⚡ Dealt " + damage + " damage!");
            
            if (effectiveness > 1.0) {
                logMessage("  ★ It's super effective!");
            } else if (effectiveness < 1.0 && effectiveness > 0) {
                logMessage("  ☆ It's not very effective...");
            } else if (effectiveness == 0) {
                logMessage("  ✕ It doesn't affect the enemy...");
            }
            
            if (enemyCurrentHP <= 0) {
                endBattle(true);
            } else {
                playerTurn = false;
                SwingUtilities.invokeLater(() -> {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    executeEnemyTurn();
                });
            }
        });
    }
    
    private void executeEnemyTurn() {
        if (battleEnded) {
            return;
        }
        
        Random rand = new Random();
        Attack attack = enemyAttacks[rand.nextInt(4)];
        
        double effectiveness = getTypeEffectiveness(attack.type, playerPokemon.getType1(), playerPokemon.getType2());
        int damage = calculateDamage(enemyPokemon.getAttack(), playerPokemon.getDefense(), attack.power, effectiveness);
        
        logMessage("───────────────────────────────────────");
        logMessage("← Foe " + enemyPokemon.getName() + " used " + attack.name + "!");
        
        animateAttack(false, () -> {
            playerCurrentHP = Math.max(0, playerCurrentHP - damage);
            updateHealthBar(true);
            
            logMessage("  ⚡ Dealt " + damage + " damage!");
            
            if (effectiveness > 1.0) {
                logMessage("  ★ It's super effective!");
            } else if (effectiveness < 1.0 && effectiveness > 0) {
                logMessage("  ☆ It's not very effective...");
            } else if (effectiveness == 0) {
                logMessage("  ✕ It doesn't affect you...");
            }
            
            if (playerCurrentHP <= 0) {
                endBattle(false);
            } else {
                playerTurn = true;
                enableAttackButtons();
            }
        });
    }
    
    private int calculateDamage(int attackStat, int defenseStat, int basePower, double effectiveness) {
        Random rand = new Random();
        double randomFactor = 0.85 + (rand.nextDouble() * 0.15); // 85% to 100%
        
        // Pokemon damage formula (simplified)
        double damage = ((2.0 * 50 / 5 + 2) * basePower * attackStat / defenseStat) / 50 + 2;
        damage = damage * effectiveness * randomFactor;
        
        return Math.max(1, (int) damage);
    }
    
    private void animateAttack(boolean isPlayerAttacking, Runnable onComplete) {
        final int[] frame = {0};
        final int maxFrames = 12;
        
        JLabel attackingSprite = isPlayerAttacking ? playerSpriteLabel : enemySpriteLabel;
        JLabel defendingSprite = isPlayerAttacking ? enemySpriteLabel : playerSpriteLabel;
        
        final Point originalPos = new Point(attackingSprite.getX(), attackingSprite.getY());
        
        animationTimer = new Timer(40, null);
        animationTimer.addActionListener(e -> {
            frame[0]++;
            
            // Shake animation for attacker
            if (frame[0] <= 6) {
                int offset = (frame[0] % 2 == 0) ? 8 : -8;
                attackingSprite.setLocation(originalPos.x + offset, originalPos.y);
            } else if (frame[0] == 7) {
                attackingSprite.setLocation(originalPos.x, originalPos.y);
            }
            
            // Flash defender on hit
            if (frame[0] >= 7 && frame[0] <= 10) {
                if (frame[0] % 2 == 0) {
                    defendingSprite.setVisible(false);
                } else {
                    defendingSprite.setVisible(true);
                }
            }
            
            if (frame[0] >= maxFrames) {
                animationTimer.stop();
                defendingSprite.setVisible(true);
                attackingSprite.setLocation(originalPos.x, originalPos.y);
                SwingUtilities.invokeLater(onComplete);
            }
        });
        animationTimer.start();
    }
    
    private void updateHealthBar(boolean isPlayer) {
        if (isPlayer) {
            playerHealthBar.setValue(playerCurrentHP);
            double percentage = ((double) playerCurrentHP / playerMaxHP) * 100;
            playerHealthBar.setString(String.format("%.0f%%", percentage));
            playerHPLabel.setText("HP: " + playerCurrentHP + "/" + playerMaxHP);
            
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
            enemyHealthBar.setString(String.format("%.0f%%", percentage));
            enemyHPLabel.setText("HP: " + enemyCurrentHP + "/" + enemyMaxHP);
            
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
        
        logMessage("═══════════════════════════════════════");
        if (playerWon) {
            logMessage("★ Foe " + enemyPokemon.getName() + " fainted!");
            logMessage("★★★ VICTORY! ★★★");
            logMessage(playerPokemon.getName() + " gained experience!");
            JOptionPane.showMessageDialog(this, 
                "🎉 Congratulations!\n" + playerPokemon.getName() + " defeated " + enemyPokemon.getName() + "!",
                "Victory!", JOptionPane.INFORMATION_MESSAGE);
        } else {
            logMessage("✕ " + playerPokemon.getName() + " fainted!");
            logMessage("✕✕✕ DEFEAT ✕✕✕");
            logMessage("You rushed to the Pokémon Center...");
            JOptionPane.showMessageDialog(this, 
                "💔 Oh no!\n" + playerPokemon.getName() + " was defeated by " + enemyPokemon.getName() + "!",
                "Defeat", JOptionPane.WARNING_MESSAGE);
        }
        logMessage("═══════════════════════════════════════");
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
    
    private void updateButtonStates() {
        if (battleEnded || !playerTurn) {
            disableAttackButtons();
        } else {
            enableAttackButtons();
        }
    }
    
    private void logMessage(String message) {
        battleLog.append(message + "\n");
        battleLog.setCaretPosition(battleLog.getDocument().getLength());
    }
    
    private ImageIcon loadPokemonSprite(int id, boolean isEnemy) {
        String dir = isEnemy ? FRONT_IMAGE_DIR : BACK_IMAGE_DIR;
        String file = dir + id + ".png";
        File f = new File(file);
        
        if (!f.exists()) {
            LOGGER.log(Level.WARNING, "Sprite not found: " + file);
            return createPlaceholderSprite(isEnemy);
        }
        
        ImageIcon icon = new ImageIcon(file);
        Image img = icon.getImage().getScaledInstance(180, 180, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }
    
    private ImageIcon createPlaceholderSprite(boolean isEnemy) {
        BufferedImage placeholder = new BufferedImage(180, 180, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = placeholder.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(new Color(200, 200, 200));
        g2d.fillOval(30, 30, 120, 120);
        
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawOval(30, 30, 120, 120);
        
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        String text = isEnemy ? "ENEMY" : "PLAYER";
        FontMetrics fm = g2d.getFontMetrics();
        int x = (180 - fm.stringWidth(text)) / 2;
        g2d.drawString(text, x, 95);
        
        g2d.dispose();
        return new ImageIcon(placeholder);
    }
    
    private Attack[] generateAttacks(Pokemon pokemon) {
        String type1 = pokemon.getType1();
        String type2 = pokemon.getType2();
        
        Map<String, Attack[]> typeAttacks = getTypeAttacks();
        Attack[] attacks = new Attack[4];
        
        // Get type-specific attacks
        Attack[] type1Attacks = typeAttacks.getOrDefault(type1, typeAttacks.get("Normal"));
        attacks[0] = type1Attacks[0];
        attacks[1] = type1Attacks[1];
        
        // If has second type, use it for third attack
        if (type2 != null && !type2.isEmpty() && !type2.equals("None") && typeAttacks.containsKey(type2)) {
            Attack[] type2Attacks = typeAttacks.get(type2);
            attacks[2] = type2Attacks[0];
        } else {
            attacks[2] = type1Attacks[2];
        }
        
        // Last attack is a normal move
        attacks[3] = typeAttacks.get("Normal")[0];
        
        return attacks;
    }
    
    private Map<String, Attack[]> getTypeAttacks() {
        Map<String, Attack[]> attacks = new HashMap<>();
        
        attacks.put("Normal", new Attack[]{
            new Attack("Tackle", "Normal", 40),
            new Attack("Body Slam", "Normal", 85),
            new Attack("Hyper Beam", "Normal", 150)
        });
        
        attacks.put("Fire", new Attack[]{
            new Attack("Ember", "Fire", 40),
            new Attack("Flamethrower", "Fire", 90),
            new Attack("Fire Blast", "Fire", 110)
        });
        
        attacks.put("Water", new Attack[]{
            new Attack("Water Gun", "Water", 40),
            new Attack("Surf", "Water", 90),
            new Attack("Hydro Pump", "Water", 110)
        });
        
        attacks.put("Electric", new Attack[]{
            new Attack("Thunder Shock", "Electric", 40),
            new Attack("Thunderbolt", "Electric", 90),
            new Attack("Thunder", "Electric", 110)
        });
        
        attacks.put("Grass", new Attack[]{
            new Attack("Vine Whip", "Grass", 45),
            new Attack("Razor Leaf", "Grass", 55),
            new Attack("Solar Beam", "Grass", 120)
        });
        
        attacks.put("Ice", new Attack[]{
            new Attack("Ice Beam", "Ice", 90),
            new Attack("Blizzard", "Ice", 110),
            new Attack("Ice Punch", "Ice", 75)
        });
        
        attacks.put("Fighting", new Attack[]{
            new Attack("Karate Chop", "Fighting", 50),
            new Attack("Low Kick", "Fighting", 65),
            new Attack("High Jump Kick", "Fighting", 130)
        });
        
        attacks.put("Poison", new Attack[]{
            new Attack("Poison Sting", "Poison", 15),
            new Attack("Sludge Bomb", "Poison", 90),
            new Attack("Toxic", "Poison", 85)
        });
        
        attacks.put("Ground", new Attack[]{
            new Attack("Dig", "Ground", 80),
            new Attack("Earthquake", "Ground", 100),
            new Attack("Bone Club", "Ground", 65)
        });
        
        attacks.put("Flying", new Attack[]{
            new Attack("Wing Attack", "Flying", 60),
            new Attack("Drill Peck", "Flying", 80),
            new Attack("Sky Attack", "Flying", 140)
        });
        
        attacks.put("Psychic", new Attack[]{
            new Attack("Confusion", "Psychic", 50),
            new Attack("Psychic", "Psychic", 90),
            new Attack("Dream Eater", "Psychic", 100)
        });
        
        attacks.put("Bug", new Attack[]{
            new Attack("Twineedle", "Bug", 25),
            new Attack("Pin Missile", "Bug", 25),
            new Attack("Megahorn", "Bug", 120)
        });
        
        attacks.put("Rock", new Attack[]{
            new Attack("Rock Throw", "Rock", 50),
            new Attack("Rock Slide", "Rock", 75),
            new Attack("Stone Edge", "Rock", 100)
        });
        
        attacks.put("Ghost", new Attack[]{
            new Attack("Lick", "Ghost", 30),
            new Attack("Shadow Ball", "Ghost", 80),
            new Attack("Night Shade", "Ghost", 100)
        });
        
        attacks.put("Dragon", new Attack[]{
            new Attack("Dragon Rage", "Dragon", 90),
            new Attack("Draco Meteor", "Dragon", 130),
            new Attack("Outrage", "Dragon", 120)
        });
        
        attacks.put("Dark", new Attack[]{
            new Attack("Bite", "Dark", 60),
            new Attack("Crunch", "Dark", 80),
            new Attack("Dark Pulse", "Dark", 80)
        });
        
        attacks.put("Steel", new Attack[]{
            new Attack("Iron Tail", "Steel", 100),
            new Attack("Metal Claw", "Steel", 50),
            new Attack("Flash Cannon", "Steel", 80)
        });
        
        attacks.put("Fairy", new Attack[]{
            new Attack("Fairy Wind", "Fairy", 40),
            new Attack("Moonblast", "Fairy", 95),
            new Attack("Dazzling Gleam", "Fairy", 80)
        });
        
        return attacks;
    }
    
    private double getTypeEffectiveness(String attackType, String defType1, String defType2) {
        double effectiveness = getTypeMult(attackType, defType1);
        if (defType2 != null && !defType2.isEmpty() && !defType2.equals("None")) {
            effectiveness *= getTypeMult(attackType, defType2);
        }
        return effectiveness;
    }
    
    private double getTypeMult(String attackType, String defenseType) {
        Map<String, Map<String, Double>> typeChart = new HashMap<>();
        
        // Initialize with neutral (1.0) and add specific interactions
        Map<String, Double> normal = new HashMap<>();
        normal.put("Rock", 0.5);
        normal.put("Ghost", 0.0);
        normal.put("Steel", 0.5);
        typeChart.put("Normal", normal);
        
        Map<String, Double> fire = new HashMap<>();
        fire.put("Fire", 0.5);
        fire.put("Water", 0.5);
        fire.put("Grass", 2.0);
        fire.put("Ice", 2.0);
        fire.put("Bug", 2.0);
        fire.put("Rock", 0.5);
        fire.put("Dragon", 0.5);
        fire.put("Steel", 2.0);
        typeChart.put("Fire", fire);
        
        Map<String, Double> water = new HashMap<>();
        water.put("Fire", 2.0);
        water.put("Water", 0.5);
        water.put("Grass", 0.5);
        water.put("Ground", 2.0);
        water.put("Rock", 2.0);
        water.put("Dragon", 0.5);
        typeChart.put("Water", water);
        
        Map<String, Double> electric = new HashMap<>();
        electric.put("Water", 2.0);
        electric.put("Electric", 0.5);
        electric.put("Grass", 0.5);
        electric.put("Ground", 0.0);
        electric.put("Flying", 2.0);
        electric.put("Dragon", 0.5);
        typeChart.put("Electric", electric);
        
        Map<String, Double> grass = new HashMap<>();
        grass.put("Fire", 0.5);
        grass.put("Water", 2.0);
        grass.put("Grass", 0.5);
        grass.put("Poison", 0.5);
        grass.put("Ground", 2.0);
        grass.put("Flying", 0.5);
        grass.put("Bug", 0.5);
        grass.put("Rock", 2.0);
        grass.put("Dragon", 0.5);
        grass.put("Steel", 0.5);
        typeChart.put("Grass", grass);
        
        Map<String, Double> ice = new HashMap<>();
        ice.put("Fire", 0.5);
        ice.put("Water", 0.5);
        ice.put("Grass", 2.0);
        ice.put("Ice", 0.5);
        ice.put("Ground", 2.0);
        ice.put("Flying", 2.0);
        ice.put("Dragon", 2.0);
        ice.put("Steel", 0.5);
        typeChart.put("Ice", ice);
        
        Map<String, Double> fighting = new HashMap<>();
        fighting.put("Normal", 2.0);
        fighting.put("Ice", 2.0);
        fighting.put("Poison", 0.5);
        fighting.put("Flying", 0.5);
        fighting.put("Psychic", 0.5);
        fighting.put("Bug", 0.5);
        fighting.put("Rock", 2.0);
        fighting.put("Ghost", 0.0);
        fighting.put("Dark", 2.0);
        fighting.put("Steel", 2.0);
        fighting.put("Fairy", 0.5);
        typeChart.put("Fighting", fighting);
        
        Map<String, Double> poison = new HashMap<>();
        poison.put("Grass", 2.0);
        poison.put("Poison", 0.5);
        poison.put("Ground", 0.5);
        poison.put("Rock", 0.5);
        poison.put("Ghost", 0.5);
        poison.put("Steel", 0.0);
        poison.put("Fairy", 2.0);
        typeChart.put("Poison", poison);
        
        Map<String, Double> ground = new HashMap<>();
        ground.put("Fire", 2.0);
        ground.put("Electric", 2.0);
        ground.put("Grass", 0.5);
        ground.put("Poison", 2.0);
        ground.put("Flying", 0.0);
        ground.put("Bug", 0.5);
        ground.put("Rock", 2.0);
        ground.put("Steel", 2.0);
        typeChart.put("Ground", ground);
        
        Map<String, Double> flying = new HashMap<>();
        flying.put("Electric", 0.5);
        flying.put("Grass", 2.0);
        flying.put("Fighting", 2.0);
        flying.put("Bug", 2.0);
        flying.put("Rock", 0.5);
        flying.put("Steel", 0.5);
        typeChart.put("Flying", flying);
        
        Map<String, Double> psychic = new HashMap<>();
        psychic.put("Fighting", 2.0);
        psychic.put("Poison", 2.0);
        psychic.put("Psychic", 0.5);
        psychic.put("Dark", 0.0);
        psychic.put("Steel", 0.5);
        typeChart.put("Psychic", psychic);
        
        Map<String, Double> bug = new HashMap<>();
        bug.put("Fire", 0.5);
        bug.put("Grass", 2.0);
        bug.put("Fighting", 0.5);
        bug.put("Poison", 0.5);
        bug.put("Flying", 0.5);
        bug.put("Psychic", 2.0);
        bug.put("Ghost", 0.5);
        bug.put("Dark", 2.0);
        bug.put("Steel", 0.5);
        bug.put("Fairy", 0.5);
        typeChart.put("Bug", bug);
        
        Map<String, Double> rock = new HashMap<>();
        rock.put("Fire", 2.0);
        rock.put("Ice", 2.0);
        rock.put("Fighting", 0.5);
        rock.put("Ground", 0.5);
        rock.put("Flying", 2.0);
        rock.put("Bug", 2.0);
        rock.put("Steel", 0.5);
        typeChart.put("Rock", rock);
        
        Map<String, Double> ghost = new HashMap<>();
        ghost.put("Normal", 0.0);
        ghost.put("Psychic", 2.0);
        ghost.put("Ghost", 2.0);
        ghost.put("Dark", 0.5);
        typeChart.put("Ghost", ghost);
        
        Map<String, Double> dragon = new HashMap<>();
        dragon.put("Dragon", 2.0);
        dragon.put("Steel", 0.5);
        dragon.put("Fairy", 0.0);
        typeChart.put("Dragon", dragon);
        
        Map<String, Double> dark = new HashMap<>();
        dark.put("Fighting", 0.5);
        dark.put("Psychic", 2.0);
        dark.put("Ghost", 2.0);
        dark.put("Dark", 0.5);
        dark.put("Fairy", 0.5);
        typeChart.put("Dark", dark);
        
        Map<String, Double> steel = new HashMap<>();
        steel.put("Fire", 0.5);
        steel.put("Water", 0.5);
        steel.put("Electric", 0.5);
        steel.put("Ice", 2.0);
        steel.put("Rock", 2.0);
        steel.put("Steel", 0.5);
        steel.put("Fairy", 2.0);
        typeChart.put("Steel", steel);
        
        Map<String, Double> fairy = new HashMap<>();
        fairy.put("Fire", 0.5);
        fairy.put("Fighting", 2.0);
        fairy.put("Poison", 0.5);
        fairy.put("Dragon", 2.0);
        fairy.put("Dark", 2.0);
        fairy.put("Steel", 0.5);
        typeChart.put("Fairy", fairy);
        
        if (typeChart.containsKey(attackType)) {
            return typeChart.get(attackType).getOrDefault(defenseType, 1.0);
        }
        return 1.0;
    }
    
    private String getTypeEmoji(String type) {
        return switch (type) {
            case "Fire" -> "🔥";
            case "Water" -> "💧";
            case "Electric" -> "⚡";
            case "Grass" -> "🍃";
            case "Ice" -> "❄️";
            case "Fighting" -> "👊";
            case "Poison" -> "☠️";
            case "Ground" -> "⛰️";
            case "Flying" -> "🦅";
            case "Psychic" -> "🔮";
            case "Bug" -> "🐛";
            case "Rock" -> "🪨";
            case "Ghost" -> "👻";
            case "Dragon" -> "🐉";
            case "Dark" -> "🌑";
            case "Steel" -> "⚙️";
            case "Fairy" -> "✨";
            default -> "⭐";
        };
    }
    
    private void returnToPokedex() {
        // Clean up
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        
        // Return to Pokedex
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
                frame.setSize(1000, 800);
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
        
        // Draw gradient background
        GradientPaint gp = new GradientPaint(0, 0, new Color(135, 206, 235), 
                                             0, getHeight(), new Color(176, 224, 230));
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, getWidth(), getHeight());
    }
    
    // Inner class for Attack
    private static class Attack {
        String name;
        String type;
        int power;
        
        Attack(String name, String type, int power) {
            this.name = name;
            this.type = type;
            this.power = power;
        }
    }
}