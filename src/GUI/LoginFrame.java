package GUI;

import database.DatabaseConnection;
import java.awt.*;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import model.User;

public class LoginFrame extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(LoginFrame.class.getName());
    private static final String DB_NAME = "Usuarios.db";
    private static final String POKEDEX_DB = "pokedex.db";

    private JTextField userField;
    private JPasswordField passField;
    private JPasswordField confirmPassField;
    private JLabel confirmPassLabel;
    private JPanel formPanel;
    private JButton loginButton, registerButton, clearButton;
    private boolean isRegisterMode = false;
    private Timer shakeTimer;

    public LoginFrame() {
        setTitle("Login - Pokédex");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 450);
        setLocationRelativeTo(null);
        setResizable(false);

        initializeUI();
    }

    private void initializeUI() {
        JPanel mainPanel = new UIUtils.GradientPanel();
        mainPanel.setLayout(new BorderLayout(0, 20));
        mainPanel.setBorder(new EmptyBorder(20, 40, 20, 40));

        // Title Label
        JLabel titleLabel = UIUtils.createLabel("Pokédex");
        titleLabel.setFont(UIUtils.TITLE_FONT);
        titleLabel.setForeground(UIUtils.PRIMARY_COLOR);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Pokeball Image
        try {
            ImageIcon pokeballIcon = new ImageIcon("Images/poke-ball.png");
            Image pokeballImage = pokeballIcon.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH);
            pokeballIcon = new ImageIcon(pokeballImage);
            JLabel pokeballLabel = new JLabel(pokeballIcon);
            pokeballLabel.setHorizontalAlignment(SwingConstants.CENTER);
            mainPanel.add(pokeballLabel, BorderLayout.CENTER);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro ao carregar a imagem poke-ball.png", e);
            showError("Erro ao carregar a imagem Poké Ball. Verifique se o arquivo 'poke-ball.png' está no diretório do projeto.");
        }

        // Create a panel to hold both form and button panels
        JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
        contentPanel.setOpaque(false);

        // Form Panel
        formPanel = createFormPanel();
        contentPanel.add(formPanel, BorderLayout.CENTER);

        // Button Panel
        contentPanel.add(createButtonPanel(), BorderLayout.SOUTH);

        // Add contentPanel to mainPanel's SOUTH
        mainPanel.add(contentPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(UIUtils.createLabel("Usuário:"), gbc);
        gbc.gridx = 1;
        userField = new JTextField(15);
        userField.setFont(UIUtils.FIELD_FONT);
        userField.setToolTipText("Digite seu nome de usuário (mínimo 3 caracteres)");
        UIUtils.applyRoundedBorder(userField);
        UIUtils.addFocusEffect(userField, this::validateField);
        userField.addActionListener(e -> performAction());
        panel.add(userField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(UIUtils.createLabel("Senha:"), gbc);
        gbc.gridx = 1;
        passField = new JPasswordField(15);
        passField.setFont(UIUtils.FIELD_FONT);
        passField.setToolTipText("Digite sua senha (mínimo 6 caracteres)");
        UIUtils.applyRoundedBorder(passField);
        UIUtils.addFocusEffect(passField, this::validateField);
        passField.addActionListener(e -> performAction());
        panel.add(passField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        confirmPassLabel = UIUtils.createLabel("Confirmar Senha:");
        confirmPassLabel.setVisible(false);
        panel.add(confirmPassLabel, gbc);

        gbc.gridx = 1;
        confirmPassField = new JPasswordField(15);
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
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
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

        registerButton = UIUtils.createStyledButton("Cadastrar", e -> toggleRegisterMode(true), "Cadastrar novo usuário");
        panel.add(registerButton);

        clearButton = UIUtils.createStyledButton("Limpar", e -> clearFields(), "Limpar todos os campos");
        panel.add(clearButton);

        return panel;
    }

    private void toggleRegisterMode(boolean enable) {
        isRegisterMode = enable;
        confirmPassLabel.setVisible(enable);
        confirmPassField.setVisible(enable);

        if (enable) {
            setTitle("Cadastro - Pokédex");
            loginButton.setText("Voltar");
            registerButton.setText("Confirmar");
            registerButton.removeActionListener(registerButton.getActionListeners()[0]);
            registerButton.addActionListener(e -> performAction());
            getRootPane().setDefaultButton(registerButton);
        } else {
            setTitle("Login - Pokédex");
            loginButton.setText("Entrar");
            registerButton.setText("Cadastrar");
            registerButton.removeActionListener(registerButton.getActionListeners()[0]);
            registerButton.addActionListener(e -> toggleRegisterMode(true));
            getRootPane().setDefaultButton(loginButton);
            clearFields();
        }
        formPanel.revalidate();
        formPanel.repaint();
    }

    private void performAction() {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword()).trim();
        if (isRegisterMode) {
            String confirmPassword = new String(confirmPassField.getPassword()).trim();
            register(username, password, confirmPassword);
        } else {
            authenticate(username, password);
        }
    }

    private void authenticate(String username, String password) {
        try (Connection conn = DatabaseConnection.connect(DB_NAME)) {
            if (User.authenticate(conn, username, password)) {
                JOptionPane.showMessageDialog(this, "Login bem-sucedido!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                openPokedex(username);
            } else {
                showError("Usuário ou senha inválidos.", userField, passField);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Erro ao autenticar", ex);
            showError("Erro: " + ex.getMessage(), userField, passField);
        }
    }

    private void register(String username, String password, String confirmPassword) {
        try (Connection conn = DatabaseConnection.connect(DB_NAME)) {
            User.register(conn, username, password, confirmPassword, false);
            JOptionPane.showMessageDialog(this, "Usuário cadastrado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            toggleRegisterMode(false);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Erro ao registrar", ex);
            JTextField[] errorFields = ex.getMessage().contains("já existe") ? new JTextField[]{userField} :
                    ex.getMessage().contains("senhas") ? new JTextField[]{passField, confirmPassField} :
                            new JTextField[]{userField, passField, confirmPassField};
            showError(ex.getMessage(), errorFields);
        }
    }

    private void showError(String message, JTextField... fields) {
        JOptionPane.showMessageDialog(this, message, "Erro", JOptionPane.ERROR_MESSAGE);
        for (JTextField field : new JTextField[]{userField, passField, confirmPassField}) {
            if (field != null) {
                boolean isError = false;
                for (JTextField errorField : fields) {
                    if (field == errorField) {
                        isError = true;
                        break;
                    }
                }
                field.setBorder(UIUtils.createRoundedBorder(isError ? UIUtils.ERROR_COLOR : Color.GRAY));
            }
        }
        animateError();
    }

    private void animateError() {
        if (shakeTimer != null && shakeTimer.isRunning()) {
            return;
        }
        shakeTimer = new Timer(50, null);
        int[] shakeOffsets = {-5, 5, -3, 3, -2, 2, 0};
        int[] shakeIndex = {0};

        shakeTimer.addActionListener(e -> {
            setLocation(getLocation().x + shakeOffsets[shakeIndex[0]], getLocation().y);
            shakeIndex[0]++;
            if (shakeIndex[0] >= shakeOffsets.length) {
                shakeTimer.stop();
            }
        });
        shakeTimer.start();
    }

    private void validateField(JTextField field) {
        String text = field instanceof JPasswordField ? new String(((JPasswordField) field).getPassword()) : field.getText();
        boolean isValid = true;
        if (field == userField) {
            isValid = User.validateUsername(text);
        } else if (field == passField) {
            isValid = User.validatePassword(text, isRegisterMode);
        } else if (field == confirmPassField) {
            isValid = text.equals(new String(passField.getPassword()));
        }
        field.setBorder(UIUtils.createRoundedBorder(isValid ? Color.GRAY : UIUtils.ERROR_COLOR));
    }

    private void clearFields() {
        userField.setText("");
        passField.setText("");
        confirmPassField.setText("");
        userField.setBorder(UIUtils.createRoundedBorder(Color.GRAY));
        passField.setBorder(UIUtils.createRoundedBorder(Color.GRAY));
        confirmPassField.setBorder(UIUtils.createRoundedBorder(Color.GRAY));
    }

    private void openPokedex(String username) {
        dispose();
        try {
            Connection pokedexConn = DatabaseConnection.connect(POKEDEX_DB);
            Connection usuariosConn = DatabaseConnection.connect(DB_NAME);
            JFrame frame = new JFrame("Pokédex");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 800);
            frame.setLocationRelativeTo(null);
            frame.setContentPane(new PokedexPanel(pokedexConn, usuariosConn, frame, username));
            frame.setVisible(true);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Erro ao abrir Pokédex", ex);
            showError("Erro ao abrir a Pokédex: " + ex.getMessage(), userField, passField);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}