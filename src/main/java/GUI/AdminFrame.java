package GUI;

import database.DatabaseConnection;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import model.User;

public class AdminFrame extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(AdminFrame.class.getName());
    private final Connection pokedexConn;
    private final Connection usersConn;
    private final String username;
    private JTable userTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton addButton, editButton, deleteButton, backButton, searchButton, clearSearchButton;
    private JLabel statusLabel;

    public AdminFrame(Connection pokedexConn, Connection usersConn, String username) {
        this.pokedexConn = pokedexConn;
        this.usersConn = usersConn;
        this.username = username;

        setTitle("Pokédex - Painel de Administração");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 500);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(600, 400));

        initializeUI();
        loadUsers(null);
    }

    private void initializeUI() {
        JPanel mainPanel = new UIUtils.GradientPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        add(mainPanel);

        JLabel headerLabel = UIUtils.createLabel("Painel de Administração");
        headerLabel.setFont(UIUtils.TITLE_FONT);
        headerLabel.setForeground(UIUtils.PRIMARY_COLOR);
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        mainPanel.add(createControlPanel(), BorderLayout.NORTH);
        mainPanel.add(createTablePane(), BorderLayout.CENTER);
        mainPanel.add(createBottomPanel(), BorderLayout.SOUTH);
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBackground(UIUtils.BG_COLOR);
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIUtils.PRIMARY_COLOR, 2, true),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        controlPanel.add(createSearchPanel(), BorderLayout.NORTH);
        controlPanel.add(createButtonPanel(), BorderLayout.SOUTH);
        return controlPanel;
    }

    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.setBackground(UIUtils.BG_COLOR);

        searchPanel.add(UIUtils.createLabel("Buscar:"));
        searchField = new JTextField(15);
        searchField.setFont(UIUtils.FIELD_FONT);
        searchField.setToolTipText("Digite o nome de usuário para buscar");
        UIUtils.applyRoundedBorder(searchField);
        UIUtils.addFocusEffect(searchField, this::validateSearchField);
        searchField.addActionListener(e -> searchUsers());
        searchPanel.add(searchField);

        searchButton = UIUtils.createStyledButton("Buscar", e -> searchUsers(), "Buscar usuário");
        searchPanel.add(searchButton);

        clearSearchButton = UIUtils.createStyledButton("Limpar", e -> clearSearch(), "Limpar busca");
        searchPanel.add(clearSearchButton);

        return searchPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttonPanel.setBackground(UIUtils.BG_COLOR);

        addButton = UIUtils.createStyledButton("Adicionar", e -> openUserDialog(null), "Adicionar novo usuário");
        buttonPanel.add(addButton);

        editButton = UIUtils.createStyledButton("Editar", e -> {
            User user = getSelectedUser();
            if (user == null) {
                showError("Selecione um usuário para editar.");
                return;
            }
            openUserDialog(user);
        }, "Editar usuário selecionado");
        buttonPanel.add(editButton);

        deleteButton = UIUtils.createStyledButton("Excluir", e -> deleteUser(), "Excluir usuário selecionado");
        buttonPanel.add(deleteButton);

        return buttonPanel;
    }

    private JScrollPane createTablePane() {
        String[] columns = {"Usuário", "Admin", "Último Login", "Criado em"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        userTable = new JTable(tableModel);
        userTable.setFont(UIUtils.FIELD_FONT);
        userTable.setRowHeight(32);
        userTable.setGridColor(new Color(200, 200, 200));
        userTable.setShowGrid(true);
        userTable.setSelectionBackground(UIUtils.ACCENT_COLOR);
        userTable.setSelectionForeground(Color.WHITE);
        userTable.setBackground(Color.WHITE);
        userTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        userTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        userTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        userTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        userTable.getTableHeader().setFont(UIUtils.LABEL_FONT);
        userTable.getTableHeader().setBackground(UIUtils.PRIMARY_COLOR);
        userTable.getTableHeader().setForeground(Color.WHITE);
        userTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    User user = getSelectedUser();
                    if (user != null) {
                        openUserDialog(user);
                    } else {
                        showError("Selecione um usuário para editar.");
                    }
                }
            }
        });
        userTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 240, 250));
                }
                return c;
            }
        });
        JScrollPane scrollPane = new JScrollPane(userTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIUtils.PRIMARY_COLOR, 2, true));
        return scrollPane;
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(UIUtils.BG_COLOR);

        statusLabel = UIUtils.createLabel("Pronto");
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIUtils.PRIMARY_COLOR, 2, true),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        bottomPanel.add(statusLabel, BorderLayout.NORTH);

        backButton = UIUtils.createStyledButton("Voltar", e -> returnToPokedex(), "Voltar ao Pokédex");
        JPanel backPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        backPanel.setBackground(UIUtils.BG_COLOR);
        backPanel.add(backButton);
        bottomPanel.add(backPanel, BorderLayout.CENTER);

        return bottomPanel;
    }

    private void openUserDialog(User user) {
        JDialog dialog = new JDialog(this, user == null ? "Adicionar Usuário" : "Editar Usuário", true);
        dialog.setSize(350, 250);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JTextField usernameField = new JTextField(15);
        usernameField.setFont(UIUtils.FIELD_FONT);
        usernameField.setToolTipText("Nome de usuário (mín. 3 caracteres)");
        UIUtils.applyRoundedBorder(usernameField);
        UIUtils.addFocusEffect(usernameField, f -> validateDialogField(f, user));
        if (user != null) {
            usernameField.setText(user.getUsername());
        }

        JPasswordField passwordField = new JPasswordField(15);
        passwordField.setFont(UIUtils.FIELD_FONT);
        passwordField.setToolTipText("Senha (mín. 6 caracteres)");
        UIUtils.applyRoundedBorder(passwordField);
        UIUtils.addFocusEffect(passwordField, f -> validateDialogField(f, user));

        JCheckBox adminCheckBox = new JCheckBox("Admin");
        adminCheckBox.setFont(UIUtils.LABEL_FONT);
        adminCheckBox.setBackground(UIUtils.BG_COLOR);
        if (user != null) {
            adminCheckBox.setSelected(user.isAdmin());
        }

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(UIUtils.BG_COLOR);
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        formPanel.add(createLabeledField("Usuário:", usernameField));
        formPanel.add(Box.createVerticalStrut(10));
        formPanel.add(createLabeledField("Senha:", passwordField));
        formPanel.add(Box.createVerticalStrut(10));
        formPanel.add(adminCheckBox);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(UIUtils.BG_COLOR);
        JButton saveButton = UIUtils.createStyledButton(user == null ? "Adicionar" : "Salvar", e -> {
            String newUsername = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            boolean isAdmin = adminCheckBox.isSelected();

            try {
                if (user == null) {
                    User.addUser(usersConn, newUsername, password, isAdmin);
                    showMessage("Usuário " + newUsername + " adicionado com sucesso!");
                } else {
                    User.editUser(usersConn, user.getUsername(), newUsername, password, isAdmin);
                    showMessage("Usuário " + user.getUsername() + " atualizado para " + newUsername + "!");
                }
                loadUsers(null);
                dialog.dispose();
            } catch (SQLException ex) {
                showError("Erro: " + ex.getMessage(), dialog);
                if (ex.getMessage().contains("já existe")) {
                    usernameField.setBorder(UIUtils.createRoundedBorder(UIUtils.ERROR_COLOR));
                }
            }
        }, user == null ? "Adicionar usuário" : "Salvar alterações");
        JButton cancelButton = UIUtils.createStyledButton("Cancelar", e -> dialog.dispose(), "Cancelar");
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JPanel createLabeledField(String labelText, JTextField field) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(UIUtils.BG_COLOR);
        panel.add(UIUtils.createLabel(labelText));
        panel.add(field);
        return panel;
    }

    private void loadUsers(String searchTerm) {
        if (usersConn == null) {
            showError("Sem conexão com o banco de dados de usuários.");
            return;
        }
        statusLabel.setText("Carregando usuários...");
        tableModel.setRowCount(0);
        try {
            ArrayList<User> users = User.getUsers(usersConn, searchTerm);
            for (User user : users) {
                tableModel.addRow(new Object[]{
                    user.getUsername(),
                    user.isAdmin() ? "Sim" : "Não",
                    user.getTimeSinceLastLogin(),
                    user.getAccountCreatedFormatted()
                });
            }
            statusLabel.setText("Usuários carregados: " + users.size() + " às " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Erro ao carregar usuários", ex);
            showError("Erro ao carregar usuários: " + ex.getMessage());
        }
    }

    private void deleteUser() {
        User user = getSelectedUser();
        if (user == null) {
            showError("Selecione um usuário para excluir.");
            return;
        }
        if (user.getUsername().equals(username)) {
            showError("Não é possível excluir o usuário atual.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Excluir usuário " + user.getUsername() + "?",
            "Confirmar Exclusão", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            statusLabel.setText("Exclusão cancelada às " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            return;
        }

        try {
            User.deleteUser(usersConn, user.getUsername());
            showMessage("Usuário " + user.getUsername() + " excluído!");
            loadUsers(null);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Erro ao excluir usuário", ex);
            showError("Erro ao excluir usuário: " + ex.getMessage());
        }
    }

    private void searchUsers() {
        String searchTerm = searchField.getText().trim();
        loadUsers(searchTerm.isEmpty() ? null : searchTerm);
        statusLabel.setText("Busca por: " + (searchTerm.isEmpty() ? "Todos" : searchTerm) + " às " +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    private void clearSearch() {
        searchField.setText("");
        loadUsers(null);
        statusLabel.setText("Mostrando todos os usuários às " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    private User getSelectedUser() {
        int row = userTable.getSelectedRow();
        if (row == -1) return null;
        String username = (String) tableModel.getValueAt(row, 0);
        boolean isAdmin = "Sim".equals(tableModel.getValueAt(row, 1));
        return new User(username, "", isAdmin);
    }

    private void returnToPokedex() {
        dispose();
        SwingUtilities.invokeLater(() -> {
            try {
                Connection newPokedexConn = pokedexConn != null && !pokedexConn.isClosed() ?
                    pokedexConn : DatabaseConnection.connect("pokedex.db");
                Connection newUsersConn = usersConn != null && !usersConn.isClosed() ?
                    usersConn : DatabaseConnection.connect("Usuarios.db");
                JFrame frame = new JFrame("Pokédex");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(800, 600);
                frame.setLocationRelativeTo(null);
                frame.setContentPane(new PokedexPanel(newPokedexConn, newUsersConn, frame, username));
                frame.setVisible(true);
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Erro ao conectar ao banco de dados", ex);
                showError("Erro ao conectar ao banco de dados: " + ex.getMessage());
            }
        });
    }

    private void validateSearchField(JTextField field) {
        field.setBorder(UIUtils.createRoundedBorder(Color.GRAY));
    }

    private void validateDialogField(JTextField field, User user) {
        String text = field instanceof JPasswordField ? new String(((JPasswordField) field).getPassword()) : field.getText();
        boolean isValid = field instanceof JPasswordField ?
            User.validatePassword(text, user == null) :
            User.validateUsername(text);
        field.setBorder(UIUtils.createRoundedBorder(isValid ? Color.GRAY : UIUtils.ERROR_COLOR));
    }

    private void showError(String message, Component parent) {
        JOptionPane.showMessageDialog(parent, message, "Erro", JOptionPane.ERROR_MESSAGE);
        statusLabel.setText(message + " às " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    private void showError(String message) {
        showError(message, this);
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        statusLabel.setText(message + " às " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }
}