package frontend.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import backend.application.dto.BattleStateDTO;
import backend.application.dto.PokemonDTO;
import backend.domain.model.Pokemon;
import backend.infrastructure.network.BattleClient;
import shared.util.I18n;

/**
 * Multiplayer Setup Dialog - Automatic Matchmaking
 * Simply connect and wait for opponent
 */
public class MultiplayerSetupDialog extends JDialog implements BattleClient.BattleClientListener {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8888;
    
    private final JFrame parentFrame;
    private final String username;
    private final List<Pokemon> selectedTeam;
    private BattleClient client;
    private String opponentName;
    
    private JTextField hostField;
    private JTextField portField;
    private JLabel statusLabel;
    private JButton connectButton;
    private boolean isConnected = false;
    
    public MultiplayerSetupDialog(JFrame parentFrame, String username, List<Pokemon> selectedTeam) {
        super(parentFrame, I18n.get("multiplayer.title"), true);
        this.parentFrame = parentFrame;
        this.username = username;
        this.selectedTeam = selectedTeam;
        
        initializeUI();
        setSize(500, 450);
        setLocationRelativeTo(parentFrame);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        // Cleanup on close
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                cleanup();
            }
        });
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(240, 245, 255));
        
        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        
        JLabel titleLabel = new JLabel(I18n.get("multiplayer.title"));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(52, 152, 219));
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);
        headerPanel.add(titleLabel);
        
        headerPanel.add(Box.createVerticalStrut(5));
        
        JLabel subtitleLabel = new JLabel(I18n.get("multiplayer.subtitle"));
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(100, 100, 100));
        subtitleLabel.setAlignmentX(CENTER_ALIGNMENT);
        headerPanel.add(subtitleLabel);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // Main content
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        // Connection section
        mainPanel.add(createConnectionPanel());
        mainPanel.add(Box.createVerticalStrut(15));
        
        // Status section
        statusLabel = new JLabel(I18n.get("multiplayer.status.disconnected"));
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        statusLabel.setForeground(Color.RED);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setAlignmentX(CENTER_ALIGNMENT);
        mainPanel.add(statusLabel);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Bottom buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        bottomPanel.setOpaque(false);
        
        JButton cancelButton = createStyledButton(I18n.get("common.cancel"), new Color(150, 150, 150));
        cancelButton.addActionListener(e -> {
            cleanup();
            dispose();
        });
        bottomPanel.add(cancelButton);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(52, 152, 219), 2),
            I18n.get("multiplayer.section.connection"),
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            new Color(52, 152, 219)
        ));
        
        // Host
        JLabel hostLabel = new JLabel(I18n.get("multiplayer.label.host"));
        hostLabel.setFont(new Font("Arial", Font.BOLD, 12));
        panel.add(hostLabel);
        
        hostField = new JTextField(DEFAULT_HOST);
        hostField.setFont(new Font("Arial", Font.PLAIN, 12));
        panel.add(hostField);
        
        // Port
        JLabel portLabel = new JLabel(I18n.get("multiplayer.label.port"));
        portLabel.setFont(new Font("Arial", Font.BOLD, 12));
        panel.add(portLabel);
        
        portField = new JTextField(String.valueOf(DEFAULT_PORT));
        portField.setFont(new Font("Arial", Font.PLAIN, 12));
        panel.add(portField);
        
        // Connect button
        panel.add(new JLabel()); // Empty cell
        
        connectButton = createStyledButton(I18n.get("multiplayer.button.connect"), new Color(76, 175, 80));
        connectButton.addActionListener(e -> connectToServer());
        panel.add(connectButton);
        
        return panel;
    }
    
    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(color.brighter());
                }
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(color);
            }
        });
        
        return button;
    }
    
    private void connectToServer() {
        String host = hostField.getText().trim();
        int port;
        
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                I18n.get("multiplayer.error.invalidPort"),
                I18n.get("common.error"),
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        updateStatus(I18n.get("multiplayer.status.connecting"), Color.ORANGE);
        
        client = new BattleClient();
        client.setListener(this);
        
        if (client.connect(host, port, username)) {
            isConnected = true;
            updateStatus(I18n.get("multiplayer.status.waitingForOpponent"), Color.ORANGE);
            
            // Disable connection controls
            connectButton.setEnabled(false);
            hostField.setEnabled(false);
            portField.setEnabled(false);
            
            // Automatically start matchmaking
            List<PokemonDTO> teamDTOs = convertTeamToDTOs(selectedTeam);
            client.createGame(username, teamDTOs); // Uses automatic matchmaking
        } else {
            updateStatus(I18n.get("multiplayer.error.serverNotFound"), Color.RED);
            JOptionPane.showMessageDialog(this,
                I18n.get("multiplayer.error.serverNotFound"),
                I18n.get("common.error"),
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private List<PokemonDTO> convertTeamToDTOs(List<Pokemon> team) {
        List<PokemonDTO> dtos = new ArrayList<>();
        for (Pokemon p : team) {
            dtos.add(new PokemonDTO(
                p.getId(), p.getName(), p.getForm(),
                p.getType1(), p.getType2(), p.getTotal(),
                p.getHp(), p.getAttack(), p.getDefense(),
                p.getSpAtk(), p.getSpDef(), p.getSpeed(),
                p.getGeneration()
            ));
        }
        return dtos;
    }
    
    private void updateStatus(String message, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(message);
            statusLabel.setForeground(color);
        });
    }
    
    private void cleanup() {
        if (client != null && client.isConnected()) {
            client.disconnect();
        }
    }
    
    // BattleClientListener implementation
    
    @Override
    public void onGameCreated(String gameId) {
        // Waiting for opponent - status already shown
        SwingUtilities.invokeLater(() -> {
            updateStatus(I18n.get("multiplayer.status.waitingForOpponent"), Color.ORANGE);
        });
    }
    
    @Override
    public void onGameJoined(String gameId, String opponentName) {
        SwingUtilities.invokeLater(() -> {
            this.opponentName = opponentName;
            updateStatus(I18n.get("multiplayer.status.opponentJoined", opponentName), new Color(76, 175, 80));
        });
    }
    
    @Override
    public void onBattleStarted(BattleStateDTO initialState) {
        SwingUtilities.invokeLater(() -> {
            dispose();
            
            // Launch multiplayer battle panel
            parentFrame.getContentPane().removeAll();
            parentFrame.setContentPane(new MultiplayerBattlePanel(
                client,
                client.isPlayerOne(),
                username,
                this.opponentName,
                parentFrame,
                initialState
            ));
            parentFrame.revalidate();
            parentFrame.repaint();
        });
    }
    
    @Override
    public void onBattleStateUpdate(BattleStateDTO state, String actionMessage) {
        // Not used in setup dialog
    }
    
    @Override
    public void onTurnComplete() {
        // Not used in setup dialog
    }
    
    @Override
    public void onBattleEnd(boolean didIWin, String winnerName, String loserName, backend.infrastructure.network.NetworkProtocol.BattleOutcomeType outcomeType) {
        // Not used in setup dialog
    }
    
    @Override
    public void onError(String errorCode, String errorMessage) {
        SwingUtilities.invokeLater(() -> {
            updateStatus(I18n.get("multiplayer.error.connectionFailed"), Color.RED);
            JOptionPane.showMessageDialog(this,
                errorMessage,
                I18n.get("common.error"),
                JOptionPane.ERROR_MESSAGE);
        });
    }
    
    @Override
    public void onGameError(String error) {
        SwingUtilities.invokeLater(() -> {
            updateStatus(error, Color.RED);
            JOptionPane.showMessageDialog(this,
                error,
                I18n.get("common.error"),
                JOptionPane.ERROR_MESSAGE);
        });
    }
    
    @Override
    public void onConnectionLost() {
        SwingUtilities.invokeLater(() -> {
            updateStatus(I18n.get("multiplayer.status.disconnected"), Color.RED);
            isConnected = false;
            
            connectButton.setEnabled(true);
            hostField.setEnabled(true);
            portField.setEnabled(true);
        });
    }
}