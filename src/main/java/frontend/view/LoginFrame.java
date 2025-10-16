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

import javax.swing.ImageIcon;
import javax.swing.JButton;
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

import backend.application.service.UserService;
import backend.infrastructure.ServiceLocator;
import frontend.util.UIUtils;

public class LoginFrame extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(LoginFrame.class.getName());

    private final UserService userService;

    private JTextField userField;
    private JPasswordField passField;
    private JPasswordField confirmPassField;
    private JLabel confirmPassLabel;
    private JPanel formPanel;
    private JButton loginButton, registerButton, clearButton;
    private JLabel statusLabel;
    private boolean isRegisterMode = false;
    private Timer shakeTimer;
    private boolean isProcessing = false;

    public LoginFrame() {
        this.userService = ServiceLocator.getInstance().getUserService();

        setTitle("Pokédex - Sistema de Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 650);
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

        JLabel titleLabel = UIUtils.createLabel("Pokédex");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(UIUtils.PRIMARY_COLOR);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel subtitleLabel = UIUtils.createLabel("Sistema de Gerenciamento");
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
        JLabel userLabel = UIUtils.createLabel("Usuário:");
        userLabel.setFont(new Font("Arial", Font.BOLD, 13));
        panel.add(userLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        userField = new JTextField();
        userField.setPreferredSize(new Dimension(250, 36));
        userField.setMinimumSize(new Dimension(250, 36));
        userField.setMaximumSize(new Dimension(250, 36));
        userField.setFont(UIUtils.FIELD_FONT);
        userField.setToolTipText("Digite seu nome de usuário (mínimo 3 caracteres)");
        UIUtils.applyRoundedBorder(userField);
        UIUtils.addFocusEffect(userField, this::validateField);
        userField.addActionListener(e -> performAction());
        panel.add(userField, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        JLabel passLabel = UIUtils.createLabel("Senha:");
        passLabel.setFont(new Font("Arial", Font.BOLD, 13));
        panel.add(passLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        passField = new JPasswordField();
        passField.setPreferredSize(new Dimension(250, 36));
        passField.setMinimumSize(new Dimension(250, 36));
        passField.setMaximumSize(new Dimension(250, 36));
        passField.setFont(UIUtils.FIELD_FONT);
        passField.setToolTipText("Digite sua senha (mínimo 6 caracteres)");
        UIUtils.applyRoundedBorder(passField);
        UIUtils.addFocusEffect(passField, this::validateField);
        passField.addActionListener(e -> performAction());
        panel.add(passField, gbc);

        // Confirm Password (initially hidden)
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        confirmPassLabel = UIUtils.createLabel("Confirmar:");
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
        confirmPassField.setToolTipText("Confirme sua senha");
        UIUtils.applyRoundedBorder(confirmPassField);
        UIUtils.addFocusEffect(confirmPassField, this::validateField);
        confirmPassField.addActionListener(e -> performAction());
        confirmPassField.setVisible(false);
        panel.add(confirmPassField, gbc);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        panel.setOpaque(false);

        loginButton = UIUtils.createStyledButton("Entrar", e -> {
            if (isRegisterMode) {
                toggleRegisterMode(false);
            } else {
                performAction();
            }
        }, "Entrar no sistema");
        panel.add(loginButton);
        getRootPane().setDefaultButton(loginButton);

        registerButton = UIUtils.createStyledButton("Cadastrar", e -> {
            if (!isRegisterMode) {
                toggleRegisterMode(true);
            } else {
                performAction();
            }
        }, "Cadastrar novo usuário");
        panel.add(registerButton);

        clearButton = UIUtils.createStyledButton("Limpar", e -> clearFields(), "Limpar todos os campos");
        panel.add(clearButton);

        return panel;
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
                setTitle("Pokédex - Cadastro de Usuário");
                loginButton.setText("Voltar");
                loginButton.setToolTipText("Voltar ao login");
                registerButton.setText("Confirmar");
                registerButton.setToolTipText("Confirmar cadastro");
                getRootPane().setDefaultButton(registerButton);
                statusLabel.setText("Preencha os campos para criar sua conta");
                statusLabel.setForeground(new Color(100, 100, 100));
            } else {
                setTitle("Pokédex - Sistema de Login");
                loginButton.setText("Entrar");
                loginButton.setToolTipText("Entrar no sistema");
                registerButton.setText("Cadastrar");
                registerButton.setToolTipText("Cadastrar novo usuário");
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
            showError("Por favor, preencha todos os campos.", userField, passField);
            return;
        }

        if (!UserService.validateUsername(username)) {
            showError("Nome de usuário deve ter pelo menos 3 caracteres.", userField);
            return;
        }

        if (!UserService.validatePassword(password, isRegisterMode)) {
            showError("Senha deve ter pelo menos 6 caracteres.", passField);
            return;
        }

        if (isRegisterMode) {
            String confirmPassword = new String(confirmPassField.getPassword()).trim();
            if (confirmPassword.isEmpty()) {
                showError("Por favor, confirme sua senha.", confirmPassField);
                return;
            }
            if (!password.equals(confirmPassword)) {
                showError("As senhas não coincidem.", passField, confirmPassField);
                return;
            }
            register(username, password, confirmPassword);
        } else {
            authenticate(username, password);
        }
    }

    private void authenticate(String username, String password) {
        setProcessing(true);
        statusLabel.setText("Autenticando...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private String errorMessage = null;

            @Override
            protected Boolean doInBackground() {
                try {
                    return userService.authenticate(username, password);
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Erro ao autenticar", ex);
                    errorMessage = "Erro de conexão: " + ex.getMessage();
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        statusLabel.setText("Login bem-sucedido!");
                        JOptionPane.showMessageDialog(LoginFrame.this,
                                "Bem-vindo, " + username + "!",
                                "Login Bem-Sucedido",
                                JOptionPane.INFORMATION_MESSAGE);
                        openPokedex(username);
                    } else {
                        if (errorMessage != null) {
                            showError(errorMessage, userField, passField);
                        } else {
                            showError("Usuário ou senha inválidos.", userField, passField);
                        }
                    }
                } catch (Exception ex) {
                    showError("Erro inesperado: " + ex.getMessage());
                } finally {
                    setProcessing(false);
                }
            }
        };
        worker.execute();
    }

    private void register(String username, String password, String confirmPassword) {
        setProcessing(true);
        statusLabel.setText("Criando conta...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private String errorMessage = null;

            @Override
            protected Boolean doInBackground() {
                try {
                    userService.register(username, password, confirmPassword, false);
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
                        statusLabel.setText("Cadastro realizado com sucesso!");
                        JOptionPane.showMessageDialog(LoginFrame.this,
                                "Conta criada com sucesso!\nAgora você pode fazer login.",
                                "Cadastro Bem-Sucedido",
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
                    showError("Erro inesperado: " + ex.getMessage());
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

        JOptionPane.showMessageDialog(this, message, "Erro", JOptionPane.ERROR_MESSAGE);

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
        if (isProcessing) return;

        String text = field instanceof JPasswordField ?
                new String(((JPasswordField) field).getPassword()) : field.getText();
        boolean isValid = true;

        if (field == userField) {
            isValid = UserService.validateUsername(text);
        } else if (field == passField) {
            isValid = UserService.validatePassword(text, isRegisterMode);
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
                        "Erro ao abrir a Pokédex: " + ex.getMessage(),
                        "Erro",
                        JOptionPane.ERROR_MESSAGE);
                // Reopen login frame
                new LoginFrame().setVisible(true);
            }
        });
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
