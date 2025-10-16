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
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
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

import backend.application.service.PokemonService;
import backend.application.service.TeamService;
import backend.domain.model.Pokemon;
import backend.infrastructure.ServiceLocator;
import frontend.util.UIUtils;

public class TeamSelectionPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(TeamSelectionPanel.class.getName());
    private static final String IMAGE_DIR = "Images/Image-Pokedex/";

    private final String username;
    private final JFrame parentFrame;
    private final PokemonService pokemonService;
    private final TeamService teamService;

    private List<Pokemon> availablePokemon;
    private final List<Pokemon> selectedTeam;
    private JPanel availablePokemonPanel;
    private JPanel selectedTeamPanel;
    private JButton startBattleButton;
    private JLabel statusLabel;

    public TeamSelectionPanel(JFrame parentFrame, String username) {
        this.username = username;
        this.parentFrame = parentFrame;
        this.pokemonService = ServiceLocator.getInstance().getPokemonService();
        this.teamService = ServiceLocator.getInstance().getTeamService();
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

        JLabel titleLabel = new JLabel("SELECT YOUR TEAM");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(200, 0, 0));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(titleLabel, BorderLayout.NORTH);

        statusLabel = new JLabel("Choose 5 Pokemon for your team (0/5 selected)");
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
            "AVAILABLE POKEMON",
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
            "YOUR TEAM",
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

        startBattleButton = UIUtils.createStyledButton("START BATTLE", e -> startBattle(), "Begin battle with selected team");
        startBattleButton.setPreferredSize(new Dimension(180, 40));
        startBattleButton.setFont(new Font("Arial", Font.BOLD, 14));
        startBattleButton.setEnabled(false);
        panel.add(startBattleButton);

        JButton backButton = UIUtils.createStyledButton("Back to Pokedex", e -> returnToPokedex(), "Return to Pokedex");
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

            for (Pokemon pokemon : availablePokemon) {
                availablePokemonPanel.add(createPokemonCard(pokemon, true));
            }

            availablePokemonPanel.revalidate();
            availablePokemonPanel.repaint();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading Pokemon", e);
            JOptionPane.showMessageDialog(this, "Error loading Pokemon: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createPokemonCard(Pokemon pokemon, boolean isAvailable) {
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

    private JPanel createSelectedPokemonCard(Pokemon pokemon, int position) {
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

        JLabel hpLabel = new JLabel("HP: " + pokemon.getHp());
        hpLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        infoPanel.add(hpLabel);

        JLabel statsLabel = new JLabel("ATK: " + pokemon.getAttack() + " | DEF: " + pokemon.getDefense());
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

    private void selectPokemon(Pokemon pokemon) {
        if (selectedTeam.size() >= 5) {
            JOptionPane.showMessageDialog(this,
                "You already have 5 Pokemon in your team!\nRemove one to add another.",
                "Team Full", JOptionPane.WARNING_MESSAGE);
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

    private void deselectPokemon(Pokemon pokemon) {
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
        statusLabel.setText(String.format("Choose 5 Pokemon for your team (%d/5 selected)", count));

        if (count == 5) {
            statusLabel.setForeground(new Color(76, 175, 80));
            statusLabel.setText("Team Complete! Ready to battle!");
        } else {
            statusLabel.setForeground(new Color(50, 50, 50));
        }
    }

    private ImageIcon loadPokemonIcon(int id, int size) {
        String file = IMAGE_DIR + id + ".png";
        File f = new File(file);

        if (!f.exists()) {
            return createPlaceholderIcon(size);
        }

        ImageIcon icon = new ImageIcon(file);
        Image img = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    private ImageIcon createPlaceholderIcon(int size) {
        java.awt.image.BufferedImage placeholder = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = placeholder.createGraphics();
        g2d.setColor(Color.GRAY);
        g2d.fillOval(size/4, size/4, size/2, size/2);
        g2d.dispose();
        return new ImageIcon(placeholder);
    }

    private void startBattle() {
        if (selectedTeam.size() != 5) {
            JOptionPane.showMessageDialog(this,
                "You must select exactly 5 Pokemon!",
                "Invalid Team", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Pokemon> enemyTeam = generateEnemyTeam();

        parentFrame.getContentPane().removeAll();
        parentFrame.setContentPane(new EnhancedBattlePanel(
            selectedTeam, enemyTeam, username, parentFrame
        ));
        parentFrame.revalidate();
        parentFrame.repaint();
    }

    @SuppressWarnings("UseSpecificCatch")
    private List<Pokemon> generateEnemyTeam() {
        try {
            // Generate random team using TeamService
            var enemyTeam = teamService.generateRandomTeam("Enemy");

            // Extract Pokemon from PokemonBattleStats
            List<Pokemon> pokemonList = new ArrayList<>();
            for (var battleStats : enemyTeam.getAllPokemon()) {
                pokemonList.add(battleStats.getPokemon());
            }

            return pokemonList;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating enemy team", e);
            JOptionPane.showMessageDialog(this, "Error generating enemy team: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
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
