package frontend.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import demo.clientserver.AuthClient;
import frontend.infrastructure.FrontendServiceLocator;
import frontend.service.IUserService;
import frontend.util.UIUtils;
import shared.util.I18n;

public class LoginFrame extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(LoginFrame.class.getName());

    private final IUserService userService;

    private JTextField userField;
    private JPasswordField passField;
    private JPasswordField confirmPassField;
    private JLabel confirmPassLabel;
    private JPanel formPanel;
    private JButton loginButton, registerButton, clearButton, backToWelcomeButton;
    private JLabel statusLabel;
    private boolean isRegisterMode = false;
    private Timer shakeTimer;
    private boolean isProcessing = false;
    
    // Remote server components
    private JCheckBox remoteServerCheckbox;
    private JTextField serverHostField;
    private JTextField serverPortField;
    private JLabel serverHostLabel;
    private JLabel serverPortLabel;

    public LoginFrame() {
        this.userService = FrontendServiceLocator.getInstance().getUserService();

        setTitle(I18n.get("login.title"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 750); // Aumentado para acomodar campos de servidor
        setLocationRelativeTo(null);
        setResizable(false);

        // Set application icon if available
        try {
            ImageIcon icon = new ImageIcon("Images/poke-ball.png");
            setIconImage(icon.getImage());
        } catch (Exception e) {
            // Icon not critical, continue without it
        }

        initializeUI();
    }

    private void initializeUI() {
        JPanel mainPanel = new UIUtils.GradientPanel();
        mainPanel.setLayout(new BorderLayout(0, 15));
        mainPanel.setBorder(new EmptyBorder(30, 50, 30, 50));

        // Title Panel
        JPanel titlePanel = createTitlePanel();
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // Center Panel with Image and Form
        JPanel centerPanel = new JPanel(new BorderLayout(0, 20));
        centerPanel.setOpaque(false);

        // Pokeball Image
        JPanel imagePanel = createImagePanel();
        centerPanel.add(imagePanel, BorderLayout.NORTH);

        // Form and Buttons
        JPanel formAndButtonsPanel = new JPanel(new BorderLayout(0, 15));
        formAndButtonsPanel.setOpaque(false);

        formPanel = createFormPanel();
        formAndButtonsPanel.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = createButtonPanel();
        formAndButtonsPanel.add(buttonPanel, BorderLayout.SOUTH);

        centerPanel.add(formAndButtonsPanel, BorderLayout.CENTER);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Status Label
        statusLabel = UIUtils.createLabel(" ");
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        statusLabel.setForeground(new Color(100, 100, 100));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel createTitlePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel titleLabel = UIUtils.createLabel(I18n.get("login.app.title"));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(UIUtils.PRIMARY_COLOR);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel subtitleLabel = UIUtils.createLabel(I18n.get("login.app.subtitle"));
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(80, 80, 80));
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);

        panel.add(textPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createImagePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setOpaque(false);

        try {
            ImageIcon pokeballIcon = new ImageIcon("Images/poke-ball.png");
            Image pokeballImage = pokeballIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            pokeballIcon = new ImageIcon(pokeballImage);
            JLabel pokeballLabel = new JLabel(pokeballIcon);
            panel.add(pokeballLabel);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Imagem poke-ball.png não encontrada", e);
            // Create a colored circle as fallback
            JPanel fallbackPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setColor(UIUtils.PRIMARY_COLOR);
                    g2d.fillOval(25, 25, 50, 50);
                    g2d.setColor(Color.WHITE);
                    g2d.fillOval(42, 42, 16, 16);
                }

                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(100, 100);
                }
            };
            fallbackPanel.setOpaque(false);
            panel.add(fallbackPanel);
        }

        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Username
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel userLabel = UIUtils.createLabel(I18n.get("login.label.username"));
        userLabel.setFont(new Font("Arial", Font.BOLD, 13));
        panel.add(userLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        userField = new JTextField();
        userField.setPreferredSize(new Dimension(250, 36));
        userField.setMinimumSize(new Dimension(250, 36));
        userField.setMaximumSize(new Dimension(250, 36));
        userField.setFont(UIUtils.FIELD_FONT);
        userField.setToolTipText(I18n.get("login.tooltip.username"));
        UIUtils.applyRoundedBorder(userField);
        UIUtils.addFocusEffect(userField, this::validateField);
        userField.addActionListener(e -> performAction());
        panel.add(userField, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        JLabel passLabel = UIUtils.createLabel(I18n.get("login.label.password"));
        passLabel.setFont(new Font("Arial", Font.BOLD, 13));
        panel.add(passLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        passField = new JPasswordField();
        passField.setPreferredSize(new Dimension(250, 36));
        passField.setMinimumSize(new Dimension(250, 36));
        passField.setMaximumSize(new Dimension(250, 36));
        passField.setFont(UIUtils.FIELD_FONT);
        passField.setToolTipText(I18n.get("login.tooltip.password"));
        UIUtils.applyRoundedBorder(passField);
        UIUtils.addFocusEffect(passField, this::validateField);
        passField.addActionListener(e -> performAction());
        panel.add(passField, gbc);

        // Confirm Password (initially hidden)
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        confirmPassLabel = UIUtils.createLabel(I18n.get("login.label.confirmPassword"));
        confirmPassLabel.setFont(new Font("Arial", Font.BOLD, 13));
        confirmPassLabel.setVisible(false);
        panel.add(confirmPassLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        confirmPassField = new JPasswordField();
        confirmPassField.setPreferredSize(new Dimension(250, 36));
        confirmPassField.setMinimumSize(new Dimension(250, 36));
        confirmPassField.setMaximumSize(new Dimension(250, 36));
        confirmPassField.setFont(UIUtils.FIELD_FONT);
        confirmPassField.setToolTipText(I18n.get("login.tooltip.confirmPassword"));
        UIUtils.applyRoundedBorder(confirmPassField);
        UIUtils.addFocusEffect(confirmPassField, this::validateField);
        confirmPassField.addActionListener(e -> performAction());
        confirmPassField.setVisible(false);
        panel.add(confirmPassField, gbc);

        // Remote Server Checkbox
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        remoteServerCheckbox = new JCheckBox(I18n.get("login.remoteServer.checkbox"));
        remoteServerCheckbox.setFont(new Font("Arial", Font.BOLD, 13));
        remoteServerCheckbox.setOpaque(false);
        remoteServerCheckbox.setForeground(Color.DARK_GRAY);
        remoteServerCheckbox.addActionListener(e -> toggleRemoteServerFields());
        panel.add(remoteServerCheckbox, gbc);

        // Remote Server Host
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        serverHostLabel = UIUtils.createLabel(I18n.get("login.remoteServer.host"));
        serverHostLabel.setFont(new Font("Arial", Font.BOLD, 13));
        serverHostLabel.setVisible(false);
        panel.add(serverHostLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        serverHostField = new JTextField("localhost");
        serverHostField.setPreferredSize(new Dimension(250, 36));
        serverHostField.setMinimumSize(new Dimension(250, 36));
        serverHostField.setMaximumSize(new Dimension(250, 36));
        serverHostField.setFont(UIUtils.FIELD_FONT);
        serverHostField.setToolTipText(I18n.get("login.remoteServer.host.tooltip"));
        UIUtils.applyRoundedBorder(serverHostField);
        serverHostField.setVisible(false);
        panel.add(serverHostField, gbc);

        // Remote Server Port
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0;
        serverPortLabel = UIUtils.createLabel(I18n.get("login.remoteServer.port"));
        serverPortLabel.setFont(new Font("Arial", Font.BOLD, 13));
        serverPortLabel.setVisible(false);
        panel.add(serverPortLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        serverPortField = new JTextField("5555");
        serverPortField.setPreferredSize(new Dimension(250, 36));
        serverPortField.setMinimumSize(new Dimension(250, 36));
        serverPortField.setMaximumSize(new Dimension(250, 36));
        serverPortField.setFont(UIUtils.FIELD_FONT);
        serverPortField.setToolTipText(I18n.get("login.remoteServer.port.tooltip"));
        UIUtils.applyRoundedBorder(serverPortField);
        serverPortField.setVisible(false);
        panel.add(serverPortField, gbc);

        return panel;
    }
    
    private void toggleRemoteServerFields() {
        boolean visible = remoteServerCheckbox.isSelected();
        serverHostLabel.setVisible(visible);
        serverHostField.setVisible(visible);
        serverPortLabel.setVisible(visible);
        serverPortField.setVisible(visible);
        
        if (visible) {
            statusLabel.setText(I18n.get("login.remoteServer.enabled"));
        } else {
            statusLabel.setText(I18n.get("login.remoteServer.disabled"));
        }
        
        formPanel.revalidate();
        formPanel.repaint();
    }

    private JPanel createButtonPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setOpaque(false);

        // First row: Login, Register, and Clear buttons
        JPanel firstRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        firstRow.setOpaque(false);

        loginButton = UIUtils.createStyledButton(I18n.get("login.button.login"), e -> {
            if (isRegisterMode) {
                toggleRegisterMode(false);
            } else {
                performAction();
            }
        }, I18n.get("login.button.login.tooltip"));
        firstRow.add(loginButton);
        getRootPane().setDefaultButton(loginButton);

        registerButton = UIUtils.createStyledButton(I18n.get("login.button.register"), e -> {
            if (!isRegisterMode) {
                toggleRegisterMode(true);
            } else {
                performAction();
            }
        }, I18n.get("login.button.register.tooltip"));
        firstRow.add(registerButton);

        clearButton = UIUtils.createStyledButton(I18n.get("login.button.clear"), e -> clearFields(), I18n.get("login.button.clear.tooltip"));
        firstRow.add(clearButton);

        mainPanel.add(firstRow);

        // Second row: Back to Welcome button (centered)
        JPanel secondRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 5));
        secondRow.setOpaque(false);

        backToWelcomeButton = UIUtils.createStyledButton(I18n.get("login.button.backToWelcome"), e -> backToWelcome(), I18n.get("login.button.backToWelcome.tooltip"));
        secondRow.add(backToWelcomeButton);

        mainPanel.add(secondRow);

        return mainPanel;
    }

    private void toggleRegisterMode(boolean enable) {
        if (isProcessing) return;

        isRegisterMode = enable;

        // Clear and reset all fields first
        clearFields();

        // Update visibility and UI
        SwingUtilities.invokeLater(() -> {
            confirmPassLabel.setVisible(enable);
            confirmPassField.setVisible(enable);

            if (enable) {
                setTitle(I18n.get("login.register.title"));
                loginButton.setText(I18n.get("login.button.back"));
                loginButton.setToolTipText(I18n.get("login.button.back.tooltip"));
                registerButton.setText(I18n.get("login.button.confirm"));
                registerButton.setToolTipText(I18n.get("login.button.confirm.tooltip"));
                getRootPane().setDefaultButton(registerButton);
                statusLabel.setText(I18n.get("login.register.instruction"));
                statusLabel.setForeground(new Color(100, 100, 100));
            } else {
                setTitle(I18n.get("login.title"));
                loginButton.setText(I18n.get("login.button.login"));
                loginButton.setToolTipText(I18n.get("login.button.login.tooltip"));
                registerButton.setText(I18n.get("login.button.register"));
                registerButton.setToolTipText(I18n.get("login.button.register.tooltip"));
                getRootPane().setDefaultButton(loginButton);
                statusLabel.setText(" ");
                statusLabel.setForeground(new Color(100, 100, 100));
            }

            // Force layout update
            formPanel.revalidate();
            formPanel.repaint();

            // Request focus on first field after layout update
            SwingUtilities.invokeLater(() -> userField.requestFocusInWindow());
        });
    }

    private void performAction() {
        if (isProcessing) return;

        String username = userField.getText().trim();
        String password = new String(passField.getPassword()).trim();

        // Validate inputs
        if (username.isEmpty() || password.isEmpty()) {
            showError(I18n.get("login.error.emptyFields"), userField, passField);
            return;
        }

        if (!validateUsername(username)) {
            showError(I18n.get("login.error.usernameInvalid"), userField);
            return;
        }

        if (!validatePassword(password, isRegisterMode)) {
            showError(I18n.get("login.error.passwordInvalid"), passField);
            return;
        }

        if (isRegisterMode) {
            String confirmPassword = new String(confirmPassField.getPassword()).trim();
            if (confirmPassword.isEmpty()) {
                showError(I18n.get("login.error.confirmPasswordEmpty"), confirmPassField);
                return;
            }
            if (!password.equals(confirmPassword)) {
                showError(I18n.get("login.error.passwordMismatch"), passField, confirmPassField);
                return;
            }
            register(username, password, confirmPassword);
        } else {
            authenticate(username, password);
        }
    }

    private void authenticate(String username, String password) {
        setProcessing(true);
        
        // Check if remote mode is enabled
        boolean isRemoteMode = remoteServerCheckbox.isSelected();
        
        if (isRemoteMode) {
            statusLabel.setText(I18n.get("login.remoteServer.connecting"));
            authenticateRemote(username, password);
        } else {
            statusLabel.setText(I18n.get("login.status.authenticating"));
            authenticateLocal(username, password);
        }
    }
    
    private void authenticateLocal(String username, String password) {
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private String errorMessage = null;

            @Override
            protected Boolean doInBackground() {
                try {
                    return userService.authenticate(username, password);
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Erro ao autenticar", ex);
                    errorMessage = I18n.get("login.error.connectionFailed", ex.getMessage());
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        statusLabel.setText(I18n.get("login.status.success"));
                        JOptionPane.showMessageDialog(LoginFrame.this,
                                I18n.get("login.success.welcome", username),
                                I18n.get("login.success.title"),
                                JOptionPane.INFORMATION_MESSAGE);
                        openPokedex(username);
                    } else {
                        if (errorMessage != null) {
                            showError(errorMessage, userField, passField);
                        } else {
                            showError(I18n.get("login.error.invalidCredentials"), userField, passField);
                        }
                    }
                } catch (InterruptedException | java.util.concurrent.ExecutionException ex) {
                    LOGGER.log(Level.SEVERE, "Erro ao processar login/registro", ex);
                    showError(I18n.get("login.error.unexpected", ex.getMessage()));
                } finally {
                    setProcessing(false);
                }
            }
        };
        worker.execute();
    }
    
    private void authenticateRemote(String username, String password) {
        SwingWorker<AuthClient.AuthResult, Void> worker = new SwingWorker<>() {
            private String connectionError = null;

            @Override
            protected AuthClient.AuthResult doInBackground() {
                try {
                    String host = serverHostField.getText().trim();
                    String portStr = serverPortField.getText().trim();
                    
                    if (host.isEmpty()) {
                        connectionError = I18n.get("login.remoteServer.error.emptyHost");
                        return null;
                    }
                    
                    int port;
                    try {
                        port = Integer.parseInt(portStr);
                        if (port < 1 || port > 65535) {
                            connectionError = I18n.get("login.remoteServer.error.invalidPortRange");
                            return null;
                        }
                    } catch (NumberFormatException ex) {
                        connectionError = I18n.get("login.remoteServer.error.invalidPort", portStr);
                        return null;
                    }
                    
                    AuthClient client = new AuthClient(host, port);
                    return client.login(username, password);
                    
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Erro ao conectar ao servidor remoto", ex);
                    connectionError = I18n.get("login.remoteServer.error.connection", ex.getMessage());
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    if (connectionError != null) {
                        showError(I18n.get("login.remoteServer.error.connectionFailed", connectionError), serverHostField);
                        setProcessing(false);
                        return;
                    }
                    
                    AuthClient.AuthResult result = get();
                    
                    if (result == null) {
                        showError(I18n.get("login.remoteServer.error.connectionFailed", ""), serverHostField);
                        setProcessing(false);
                        return;
                    }
                    
                    if (result.success) {
                        statusLabel.setText(I18n.get("login.remoteServer.success"));
                        JOptionPane.showMessageDialog(LoginFrame.this,
                                I18n.get("login.remoteServer.success.message", username, result.userType),
                                I18n.get("login.remoteServer.success.title"),
                                JOptionPane.INFORMATION_MESSAGE);
                        openPokedex(username);
                    } else {
                        showError(I18n.get("login.remoteServer.error.failed", result.error), userField, passField);
                    }
                } catch (InterruptedException | java.util.concurrent.ExecutionException ex) {
                    LOGGER.log(Level.SEVERE, "Erro ao processar login remoto", ex);
                    showError(I18n.get("login.remoteServer.error.processingResponse", ex.getMessage()));
                } finally {
                    setProcessing(false);
                }
            }
        };
        worker.execute();
    }

    private void register(String username, String password, String confirmPassword) {
        setProcessing(true);
        statusLabel.setText(I18n.get("login.status.registering"));

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private String errorMessage = null;

            @Override
            protected Boolean doInBackground() {
                try {
                    // Validate password match before calling service
                    if (!password.equals(confirmPassword)) {
                        errorMessage = I18n.get("login.error.passwordMismatch");
                        return false;
                    }
                    userService.register(username, password, false);
                    return true;
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Erro ao registrar", ex);
                    errorMessage = ex.getMessage();
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        statusLabel.setText(I18n.get("login.register.success"));
                        JOptionPane.showMessageDialog(LoginFrame.this,
                                I18n.get("login.register.successMessage"),
                                I18n.get("login.register.successTitle"),
                                JOptionPane.INFORMATION_MESSAGE);
                        toggleRegisterMode(false);
                    } else {
                        JTextField[] errorFields = errorMessage.contains("já existe") ?
                                new JTextField[]{userField} :
                                errorMessage.contains("senhas") ?
                                        new JTextField[]{passField, confirmPassField} :
                                        new JTextField[]{userField, passField, confirmPassField};
                        showError(errorMessage, errorFields);
                    }
                } catch (Exception ex) {
                    showError(I18n.get("login.error.unexpected", ex.getMessage()));
                } finally {
                    setProcessing(false);
                }
            }
        };
        worker.execute();
    }

    private void setProcessing(boolean processing) {
        isProcessing = processing;
        loginButton.setEnabled(!processing);
        registerButton.setEnabled(!processing);
        clearButton.setEnabled(!processing);
        userField.setEnabled(!processing);
        passField.setEnabled(!processing);
        confirmPassField.setEnabled(!processing);

        if (!processing) {
            statusLabel.setText(" ");
        }
    }

    private void showError(String message, JTextField... fields) {
        statusLabel.setText(message);
        statusLabel.setForeground(UIUtils.ERROR_COLOR);

        JOptionPane.showMessageDialog(this, message, I18n.get("common.error"), JOptionPane.ERROR_MESSAGE);

        // Reset all borders first
        userField.setBorder(UIUtils.createCompoundRoundedBorder(Color.GRAY));
        passField.setBorder(UIUtils.createCompoundRoundedBorder(Color.GRAY));
        confirmPassField.setBorder(UIUtils.createCompoundRoundedBorder(Color.GRAY));

        // Highlight error fields
        for (JTextField field : fields) {
            if (field != null) {
                field.setBorder(UIUtils.createCompoundRoundedBorder(UIUtils.ERROR_COLOR));
            }
        }

        animateError();

        // Reset status label color after a delay
        Timer timer = new Timer(3000, e -> {
            statusLabel.setForeground(new Color(100, 100, 100));
            if (!isProcessing) {
                statusLabel.setText(" ");
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void animateError() {
        if (shakeTimer != null && shakeTimer.isRunning()) {
            return;
        }

        Point originalLocation = getLocation();
        int[] shakeOffsets = {-8, 8, -6, 6, -4, 4, -2, 2, 0};
        int[] shakeIndex = {0};

        shakeTimer = new Timer(40, null);
        shakeTimer.addActionListener(e -> {
            if (shakeIndex[0] < shakeOffsets.length) {
                setLocation(originalLocation.x + shakeOffsets[shakeIndex[0]], originalLocation.y);
                shakeIndex[0]++;
            } else {
                setLocation(originalLocation);
                shakeTimer.stop();
            }
        });
        shakeTimer.start();
    }

    private void validateField(JTextField field) {
        if (isProcessing || field == null) return;

        String text = field instanceof JPasswordField ?
                new String(((JPasswordField) field).getPassword()) : field.getText();
        boolean isValid = true;

        if (field == userField) {
            isValid = validateUsername(text);
        } else if (field == passField) {
            isValid = validatePassword(text, isRegisterMode);
        } else if (field == confirmPassField) {
            isValid = text.equals(new String(passField.getPassword()));
        }

        Color borderColor = text.isEmpty() ? Color.GRAY :
                (isValid ? new Color(40, 167, 69) : UIUtils.ERROR_COLOR);
        field.setBorder(UIUtils.createCompoundRoundedBorder(borderColor));
    }

    private void clearFields() {
        userField.setText("");
        passField.setText("");
        confirmPassField.setText("");
        userField.setBorder(UIUtils.createCompoundRoundedBorder(Color.GRAY));
        passField.setBorder(UIUtils.createCompoundRoundedBorder(Color.GRAY));
        confirmPassField.setBorder(UIUtils.createCompoundRoundedBorder(Color.GRAY));
        statusLabel.setText(" ");
        statusLabel.setForeground(new Color(100, 100, 100));

        // Force repaint to ensure text is cleared visually
        userField.revalidate();
        userField.repaint();
        passField.revalidate();
        passField.repaint();
        confirmPassField.revalidate();
        confirmPassField.repaint();
    }

    private void backToWelcome() {
        // Close current LoginFrame
        dispose();
        
        // Open WelcomeFrame
        SwingUtilities.invokeLater(() -> {
            WelcomeFrame welcomeFrame = new WelcomeFrame();
            welcomeFrame.setVisible(true);
        });
    }

    private void openPokedex(String username) {
        dispose();
        SwingUtilities.invokeLater(() -> {
            try {
                JFrame frame = new JFrame("Pokédex - " + username);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(1000, 800);
                frame.setLocationRelativeTo(null);
                frame.setContentPane(new PokedexPanel(frame, username));
                frame.setVisible(true);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Erro ao abrir Pokédex", ex);
                JOptionPane.showMessageDialog(null,
                        I18n.get("login.error.openPokedex", ex.getMessage()),
                        I18n.get("common.error"),
                        JOptionPane.ERROR_MESSAGE);
                // Reopen login frame
                new LoginFrame().setVisible(true);
            }
        });
    }

    /**
     * Validate username (must be at least 3 characters)
     */
    private static boolean validateUsername(String username) {
        return username != null && !username.trim().isEmpty() && username.trim().length() >= 3;
    }

    /**
     * Validate password
     * @param password Password to validate
     * @param isNewUser True if registering new user (requires min 6 chars), false for existing user
     */
    private static boolean validatePassword(String password, boolean isNewUser) {
        if (password == null) return false;
        if (isNewUser) {
            return !password.trim().isEmpty() && password.trim().length() >= 6;
        }
        return password.trim().isEmpty() || password.trim().length() >= 6;
    }

    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            // Use default if system LAF fails
        }

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
