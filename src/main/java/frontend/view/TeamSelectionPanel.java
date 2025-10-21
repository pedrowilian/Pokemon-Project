package frontend.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
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
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import backend.application.dto.DTOMapper;
import backend.application.dto.PokemonDTO;
import backend.domain.model.Pokemon;
import frontend.infrastructure.FrontendServiceLocator;
import frontend.service.IPokemonService;
import frontend.service.ITeamService;
import frontend.util.UIUtils;
import shared.util.I18n;

public class TeamSelectionPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(TeamSelectionPanel.class.getName());
    private static final String IMAGE_DIR = "Images/Image-Pokedex/";

    private final String username;
    private final JFrame parentFrame;
    private final IPokemonService pokemonService;
    private final ITeamService teamService;

    private List<PokemonDTO> availablePokemon;
    private final List<PokemonDTO> selectedTeam;
    private JPanel availablePokemonPanel;
    private JPanel selectedTeamPanel;
    private JButton startBattleButton;
    private JLabel statusLabel;

    public TeamSelectionPanel(JFrame parentFrame, String username) {
        this.username = username;
        this.parentFrame = parentFrame;
        this.pokemonService = FrontendServiceLocator.getInstance().getPokemonService();
        this.teamService = FrontendServiceLocator.getInstance().getTeamService();
        this.availablePokemon = new ArrayList<>();
        this.selectedTeam = new ArrayList<>();

        initializeUI();
        loadAvailablePokemon();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setBackground(new Color(240, 245, 255));

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createMainPanel(), BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        JLabel titleLabel = new JLabel(I18n.get("team.title"));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(200, 0, 0));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(titleLabel, BorderLayout.NORTH);

        statusLabel = new JLabel(I18n.get("team.status.choose", 0));
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setForeground(new Color(50, 50, 50));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        mainPanel.setOpaque(false);

        mainPanel.add(createAvailablePokemonPanel());
        mainPanel.add(createSelectedTeamPanel());

        return mainPanel;
    }

    private JPanel createAvailablePokemonPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(100, 150, 200), 2, true),
            I18n.get("team.panel.available"),
            TitledBorder.CENTER,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            new Color(100, 150, 200)
        ));

        availablePokemonPanel = new JPanel(new GridLayout(0, 6, 8, 8));
        availablePokemonPanel.setOpaque(false);
        availablePokemonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(availablePokemonPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSelectedTeamPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(76, 175, 80), 2, true),
            I18n.get("team.panel.yourTeam"),
            TitledBorder.CENTER,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            new Color(76, 175, 80)
        ));

        selectedTeamPanel = new JPanel();
        selectedTeamPanel.setLayout(new BoxLayout(selectedTeamPanel, BoxLayout.Y_AXIS));
        selectedTeamPanel.setOpaque(false);
        selectedTeamPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(selectedTeamPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        panel.setOpaque(false);

        startBattleButton = UIUtils.createStyledButton(I18n.get("team.button.startBattle"), e -> startBattle(), I18n.get("team.tooltip.startBattle"));
        startBattleButton.setPreferredSize(new Dimension(180, 40));
        startBattleButton.setFont(new Font("Arial", Font.BOLD, 14));
        startBattleButton.setEnabled(false);
        panel.add(startBattleButton);

        JButton backButton = UIUtils.createStyledButton(I18n.get("team.button.back"), e -> returnToPokedex(), I18n.get("team.tooltip.back"));
        backButton.setPreferredSize(new Dimension(160, 40));
        backButton.setFont(new Font("Arial", Font.BOLD, 12));
        panel.add(backButton);

        return panel;
    }

    @SuppressWarnings("UseSpecificCatch")
    private void loadAvailablePokemon() {
        availablePokemon.clear();
        availablePokemonPanel.removeAll();

        try {
            availablePokemon = pokemonService.getAllPokemon();

            for (PokemonDTO pokemon : availablePokemon) {
                availablePokemonPanel.add(createPokemonCard(pokemon, true));
            }

            availablePokemonPanel.revalidate();
            availablePokemonPanel.repaint();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading Pokemon", e);
            JOptionPane.showMessageDialog(this, I18n.get("team.error.loadPokemon", e.getMessage()),
                I18n.get("common.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createPokemonCard(PokemonDTO pokemon, boolean isAvailable) {
        JPanel card = new JPanel(new BorderLayout(3, 3));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        card.setPreferredSize(new Dimension(90, 110));
        card.setMaximumSize(new Dimension(90, 110));

        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setIcon(loadPokemonIcon(pokemon.getId(), 50));
        card.add(imageLabel, BorderLayout.CENTER);

        JLabel nameLabel = new JLabel("<html><center>" + pokemon.getName() + "</center></html>");
        nameLabel.setFont(new Font("Arial", Font.BOLD, 10));
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(nameLabel, BorderLayout.SOUTH);

        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isAvailable) {
                    selectPokemon(pokemon);
                } else {
                    deselectPokemon(pokemon);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIUtils.ACCENT_COLOR, 2, true),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)
                ));
            }
        });

        return card;
    }

    private JPanel createSelectedPokemonCard(PokemonDTO pokemon, int position) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(new Color(240, 255, 240));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(76, 175, 80), 2, true),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        card.setMaximumSize(new Dimension(450, 80));
        card.setPreferredSize(new Dimension(400, 80));

        JLabel positionLabel = new JLabel(String.valueOf(position));
        positionLabel.setFont(new Font("Arial", Font.BOLD, 20));
        positionLabel.setForeground(Color.WHITE);
        positionLabel.setOpaque(true);
        positionLabel.setBackground(new Color(76, 175, 80));
        positionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        positionLabel.setPreferredSize(new Dimension(40, 40));
        positionLabel.setBorder(BorderFactory.createLineBorder(new Color(56, 142, 60), 2));
        card.add(positionLabel, BorderLayout.WEST);

        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(loadPokemonIcon(pokemon.getId(), 60));
        imageLabel.setPreferredSize(new Dimension(60, 60));
        card.add(imageLabel, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(pokemon.getName());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        nameLabel.setForeground(new Color(76, 175, 80));
        infoPanel.add(nameLabel);

        infoPanel.add(Box.createVerticalStrut(3));

        JLabel hpLabel = new JLabel(I18n.get("team.label.hp", pokemon.getHp()));
        hpLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        infoPanel.add(hpLabel);

        JLabel statsLabel = new JLabel(I18n.get("team.label.stats", pokemon.getAttack(), pokemon.getDefense()));
        statsLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        infoPanel.add(statsLabel);

        card.add(infoPanel, BorderLayout.EAST);

        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                deselectPokemon(pokemon);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 20, 60), 2, true),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(76, 175, 80), 2, true),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)
                ));
            }
        });

        return card;
    }

    private void selectPokemon(PokemonDTO pokemon) {
        if (selectedTeam.size() >= 5) {
            JOptionPane.showMessageDialog(this,
                I18n.get("team.error.teamFull"),
                I18n.get("team.error.teamFullTitle"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (selectedTeam.contains(pokemon)) {
            return;
        }

        selectedTeam.add(pokemon);
        updateSelectedTeamPanel();
        updateStatusLabel();

        if (selectedTeam.size() == 5) {
            startBattleButton.setEnabled(true);
        }
    }

    private void deselectPokemon(PokemonDTO pokemon) {
        selectedTeam.remove(pokemon);
        updateSelectedTeamPanel();
        updateStatusLabel();
        startBattleButton.setEnabled(selectedTeam.size() == 5);
    }

    private void updateSelectedTeamPanel() {
        selectedTeamPanel.removeAll();

        for (int i = 0; i < selectedTeam.size(); i++) {
            selectedTeamPanel.add(createSelectedPokemonCard(selectedTeam.get(i), i + 1));
            selectedTeamPanel.add(Box.createVerticalStrut(8));
        }

        selectedTeamPanel.revalidate();
        selectedTeamPanel.repaint();
    }

    private void updateStatusLabel() {
        int count = selectedTeam.size();
        statusLabel.setText(I18n.get("team.status.choose", count));

        if (count == 5) {
            statusLabel.setForeground(new Color(76, 175, 80));
            statusLabel.setText(I18n.get("team.status.complete"));
        } else {
            statusLabel.setForeground(new Color(50, 50, 50));
        }
    }

    private ImageIcon loadPokemonIcon(int id, int size) {
        String file = IMAGE_DIR + id + ".png";
        // Use ImageCache for better performance
        return frontend.util.ImageCache.loadSync(file);
    }

    private void startBattle() {
        if (selectedTeam.size() != 5) {
            JOptionPane.showMessageDialog(this,
                I18n.get("team.error.invalidTeam"),
                I18n.get("team.error.invalidTeamTitle"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 🎮 DIALOG: Escolher modo de batalha
        showBattleModeDialog();
    }
    
    /**
     * Mostra diálogo para escolher entre Local (IA) ou Multiplayer (Online)
     */
    private void showBattleModeDialog() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(240, 245, 255));
        
        // Título
        JLabel titleLabel = new JLabel(I18n.get("team.battleMode.title"));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(new Color(200, 0, 0));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Botões
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        buttonsPanel.setOpaque(false);
        
        // Botão Local
        JButton localButton = new JButton(I18n.get("team.battleMode.local.button"));
        localButton.setFont(new Font("Arial", Font.PLAIN, 16));
        localButton.setPreferredSize(new Dimension(350, 80));
        localButton.setBackground(new Color(76, 175, 80));
        localButton.setForeground(Color.WHITE);
        localButton.setFocusPainted(false);
        localButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        localButton.addActionListener(e -> {
            // Fecha o diálogo antes de iniciar a batalha
            SwingUtilities.invokeLater(() -> {
                Window window = SwingUtilities.getWindowAncestor(panel);
                if (window != null) {
                    window.dispose();
                }
            });
            // Inicia batalha após fechar o diálogo
            SwingUtilities.invokeLater(this::startLocalBattle);
        });
        
        // Botão Multiplayer
        JButton multiplayerButton = new JButton(I18n.get("team.battleMode.multiplayer.button"));
        multiplayerButton.setFont(new Font("Arial", Font.PLAIN, 16));
        multiplayerButton.setPreferredSize(new Dimension(350, 80));
        multiplayerButton.setBackground(new Color(33, 150, 243));
        multiplayerButton.setForeground(Color.WHITE);
        multiplayerButton.setFocusPainted(false);
        multiplayerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        multiplayerButton.addActionListener(e -> {
            // Fecha o diálogo antes de mostrar o próximo
            SwingUtilities.invokeLater(() -> {
                Window window = SwingUtilities.getWindowAncestor(panel);
                if (window != null) {
                    window.dispose();
                }
            });
            // Mostra configuração multiplayer após fechar o diálogo
            SwingUtilities.invokeLater(this::showMultiplayerConfigDialog);
        });
        
        buttonsPanel.add(localButton);
        buttonsPanel.add(multiplayerButton);
        panel.add(buttonsPanel, BorderLayout.CENTER);
        
        // Mostra diálogo
        JOptionPane.showMessageDialog(this, panel, I18n.get("team.battleMode.dialog.title"), 
                                     JOptionPane.PLAIN_MESSAGE);
    }
    
    /**
     * Inicia batalha local (contra IA)
     */
    private void startLocalBattle() {
        List<Pokemon> enemyTeam = generateEnemyTeam();

        // Convert DTOs to domain models
        List<Pokemon> playerTeamDomain = selectedTeam.stream()
                .map(DTOMapper::toDomain)
                .toList();

        parentFrame.getContentPane().removeAll();
        parentFrame.setContentPane(new EnhancedBattlePanel(
            playerTeamDomain, enemyTeam, username, parentFrame
        ));
        parentFrame.revalidate();
        parentFrame.repaint();
    }
    
    /**
     * Mostra diálogo de configuração multiplayer
     */
    private void showMultiplayerConfigDialog() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel infoLabel = new JLabel(I18n.get("team.multiplayer.config.title"));
        infoLabel.setFont(new Font("Arial", Font.BOLD, 16));
        infoLabel.setAlignmentX(CENTER_ALIGNMENT);
        panel.add(infoLabel);
        panel.add(Box.createVerticalStrut(15));
        
        // Campo Host
        JPanel hostPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        hostPanel.add(new JLabel(I18n.get("team.multiplayer.config.host")));
        javax.swing.JTextField hostField = new javax.swing.JTextField("localhost", 20);
        hostPanel.add(hostField);
        panel.add(hostPanel);
        
        // Campo Porta
        JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        portPanel.add(new JLabel(I18n.get("team.multiplayer.config.port")));
        javax.swing.JTextField portField = new javax.swing.JTextField("5556", 8);
        portPanel.add(portField);
        panel.add(portPanel);
        
        panel.add(Box.createVerticalStrut(10));
        
        JLabel hintLabel = new JLabel(I18n.get("team.multiplayer.config.hints"));
        hintLabel.setAlignmentX(CENTER_ALIGNMENT);
        panel.add(hintLabel);
        
        int result = JOptionPane.showConfirmDialog(this, panel, 
                                                   I18n.get("team.multiplayer.config.dialog.title"),
                                                   JOptionPane.OK_CANCEL_OPTION,
                                                   JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            String host = hostField.getText().trim();
            int port;
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, 
                    I18n.get("team.multiplayer.config.error.invalidPort"),
                    I18n.get("common.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            startMultiplayerBattle(host, port);
        }
    }
    
    /**
     * Inicia batalha multiplayer
     */
    private void startMultiplayerBattle(String host, int port) {
        List<Pokemon> enemyTeam = generateEnemyTeam(); // Placeholder, será gerenciado pelo servidor

        // Convert DTOs to domain models
        List<Pokemon> playerTeamDomain = selectedTeam.stream()
                .map(DTOMapper::toDomain)
                .toList();

        parentFrame.getContentPane().removeAll();
        parentFrame.setContentPane(new EnhancedBattlePanel(
            playerTeamDomain, enemyTeam, username, parentFrame, host, port
        ));
        parentFrame.revalidate();
        parentFrame.repaint();
    }

    @SuppressWarnings("UseSpecificCatch")
    private List<Pokemon> generateEnemyTeam() {
        try {
            // Use IPokemonService to get random Pokemon
            List<PokemonDTO> randomPokemonDTOs = pokemonService.getRandomPokemon(5);
            
            // Temporary: Convert DTOs to domain models for EnhancedBattlePanel
            // This will be removed when EnhancedBattlePanel is refactored
            return randomPokemonDTOs.stream()
                    .map(DTOMapper::toDomain)
                    .toList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating enemy team", e);
            JOptionPane.showMessageDialog(this, I18n.get("team.error.generateEnemy", e.getMessage()),
                I18n.get("common.error"), JOptionPane.ERROR_MESSAGE);
            return new ArrayList<>();
        }
    }

    private void returnToPokedex() {
        parentFrame.dispose();
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Pokedex");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 800);
            frame.setLocationRelativeTo(null);
            frame.setContentPane(new PokedexPanel(frame, username));
            frame.setVisible(true);
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        GradientPaint gp = new GradientPaint(0, 0, new Color(240, 245, 255),
                                             0, getHeight(), new Color(200, 220, 255));
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, getWidth(), getHeight());
    }
}
