package frontend.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import backend.application.service.UserService;
import backend.domain.model.User;
import backend.infrastructure.ServiceLocator;
import shared.util.I18n;
import frontend.util.UIUtils;

public class AdminFrame extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(AdminFrame.class.getName());
    private final String username;
    private final UserService userService;
    private JTable userTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField searchField;
    private JButton addButton, editButton, deleteButton, backButton, searchButton, clearSearchButton, refreshButton;
    private JLabel statusLabel, userCountLabel;
    private boolean isProcessing = false;

    public AdminFrame(String username) {
        this.username = username;
        this.userService = ServiceLocator.getInstance().getUserService();

        setTitle(I18n.get("admin.title", username));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(950, 650);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 500));

        initializeUI();
        loadUsers(null);
    }

    private void initializeUI() {
        JPanel mainPanel = new UIUtils.GradientPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(mainPanel);

        // Header
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Control Panel
        JPanel controlPanel = createControlPanel();
        mainPanel.add(controlPanel, BorderLayout.CENTER);

        // Bottom Panel
        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel headerLabel = UIUtils.createLabel(I18n.get("admin.header.title"));
        headerLabel.setFont(new Font("Arial", Font.BOLD, 28));
        headerLabel.setForeground(UIUtils.PRIMARY_COLOR);
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel subheaderLabel = UIUtils.createLabel(I18n.get("admin.header.subtitle"));
        subheaderLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        subheaderLabel.setForeground(new Color(100, 100, 100));
        subheaderLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 3));
        textPanel.setOpaque(false);
        textPanel.add(headerLabel);
        textPanel.add(subheaderLabel);

        panel.add(textPanel, BorderLayout.CENTER);

        // User count label
        userCountLabel = UIUtils.createLabel(" ");
        userCountLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        userCountLabel.setForeground(new Color(80, 80, 80));
        userCountLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(userCountLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout(0, 10));
        controlPanel.setOpaque(false);

        // Search and action panel
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIUtils.PRIMARY_COLOR, 2, true),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        topPanel.setBackground(UIUtils.BG_COLOR);

        topPanel.add(createSearchPanel(), BorderLayout.NORTH);
        topPanel.add(createButtonPanel(), BorderLayout.CENTER);

        controlPanel.add(topPanel, BorderLayout.NORTH);
        controlPanel.add(createTablePane(), BorderLayout.CENTER);

        return controlPanel;
    }

    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        searchPanel.setOpaque(false);

        JLabel searchLabel = UIUtils.createLabel(I18n.get("admin.label.search"));
        searchLabel.setFont(new Font("Arial", Font.BOLD, 13));
        searchPanel.add(searchLabel);

        searchField = new JTextField(20);
        searchField.setFont(UIUtils.FIELD_FONT);
        searchField.setToolTipText(I18n.get("admin.tooltip.search"));
        UIUtils.applyRoundedBorder(searchField);
        searchField.addActionListener(e -> searchUsers());
        // Real-time search
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                performQuickFilter();
            }
        });
        searchPanel.add(searchField);

        searchButton = UIUtils.createStyledButton(I18n.get("admin.button.search"), e -> searchUsers(), I18n.get("admin.tooltip.searchButton"));
        searchPanel.add(searchButton);

        clearSearchButton = UIUtils.createStyledButton(I18n.get("admin.button.clear"), e -> clearSearch(), I18n.get("admin.tooltip.clear"));
        searchPanel.add(clearSearchButton);

        refreshButton = UIUtils.createStyledButton(I18n.get("admin.button.refresh"), e -> refreshTable(), I18n.get("admin.tooltip.refresh"));
        searchPanel.add(refreshButton);

        return searchPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        buttonPanel.setOpaque(false);

        JLabel actionsLabel = UIUtils.createLabel(I18n.get("admin.label.actions"));
        actionsLabel.setFont(new Font("Arial", Font.BOLD, 13));
        buttonPanel.add(actionsLabel);

        addButton = UIUtils.createStyledButton(I18n.get("admin.button.add"), e -> openUserDialog(null), I18n.get("admin.tooltip.add"));
        buttonPanel.add(addButton);

        editButton = UIUtils.createStyledButton(I18n.get("admin.button.edit"), e -> {
            User user = getSelectedUser();
            if (user == null) {
                showError(I18n.get("admin.error.selectUser"));
                return;
            }
            openUserDialog(user);
        }, I18n.get("admin.tooltip.edit"));
        buttonPanel.add(editButton);

        deleteButton = UIUtils.createStyledButton(I18n.get("admin.button.delete"), e -> deleteUser(), I18n.get("admin.tooltip.delete"));
        deleteButton.setBackground(new Color(220, 53, 69));
        buttonPanel.add(deleteButton);

        return buttonPanel;
    }

    private JScrollPane createTablePane() {
        String[] columns = {I18n.get("admin.table.username"), I18n.get("admin.table.type"), I18n.get("admin.table.lastLogin"), I18n.get("admin.table.createdAt")};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return String.class;
            }
        };

        userTable = new JTable(tableModel);
        userTable.setFont(new Font("Arial", Font.PLAIN, 13));
        userTable.setRowHeight(35);
        userTable.setGridColor(new Color(220, 220, 220));
        userTable.setShowGrid(true);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.setSelectionBackground(UIUtils.ACCENT_COLOR);
        userTable.setSelectionForeground(Color.WHITE);
        userTable.setBackground(Color.WHITE);
        userTable.setIntercellSpacing(new Dimension(10, 1));

        // Column widths
        userTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        userTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        userTable.getColumnModel().getColumn(2).setPreferredWidth(160);
        userTable.getColumnModel().getColumn(3).setPreferredWidth(130);

        // Table header
        userTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        userTable.getTableHeader().setBackground(UIUtils.PRIMARY_COLOR);
        userTable.getTableHeader().setForeground(Color.WHITE);
        userTable.getTableHeader().setPreferredSize(new Dimension(0, 40));
        userTable.getTableHeader().setReorderingAllowed(false);

        // Double-click to edit
        userTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !isProcessing) {
                    User user = getSelectedUser();
                    if (user != null) {
                        openUserDialog(user);
                    }
                }
            }
        });

        // Custom cell renderer with alternating colors
        userTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 245, 250));
                }

                // Center align "Type" column
                if (column == 1) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }

                return c;
            }
        });

        // Add sorting capability
        sorter = new TableRowSorter<>(tableModel);
        userTable.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(userTable);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIUtils.PRIMARY_COLOR, 2, true),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        scrollPane.getViewport().setBackground(Color.WHITE);

        return scrollPane;
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Status label
        statusLabel = UIUtils.createLabel(I18n.get("admin.status.ready"));
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        statusLabel.setForeground(new Color(80, 80, 80));
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(250, 250, 250));
        bottomPanel.add(statusLabel, BorderLayout.CENTER);

        // Back button
        backButton = UIUtils.createStyledButton(I18n.get("admin.button.back"), e -> returnToPokedex(),
            I18n.get("admin.tooltip.back"));
        backButton.setPreferredSize(new Dimension(180, 35));
        JPanel backPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        backPanel.setOpaque(false);
        backPanel.add(backButton);
        bottomPanel.add(backPanel, BorderLayout.SOUTH);

        return bottomPanel;
    }

    private void openUserDialog(User user) {
        if (isProcessing) return;

        boolean isEditing = user != null;
        JDialog dialog = new JDialog(this,
            isEditing ? I18n.get("admin.dialog.edit") : I18n.get("admin.dialog.add"), true);
        dialog.setSize(450, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(15, 15));
        dialog.setResizable(false);

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(UIUtils.BG_COLOR);
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Username
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel userLabel = UIUtils.createLabel(I18n.get("admin.label.username"));
        userLabel.setFont(new Font("Arial", Font.BOLD, 13));
        formPanel.add(userLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField usernameField = new JTextField(18);
        usernameField.setFont(UIUtils.FIELD_FONT);
        usernameField.setToolTipText(I18n.get("admin.tooltip.username"));
        UIUtils.applyRoundedBorder(usernameField);
        if (isEditing) {
            usernameField.setText(user.getUsername());
        }
        formPanel.add(usernameField, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        JLabel passLabel = UIUtils.createLabel(I18n.get("admin.label.password"));
        passLabel.setFont(new Font("Arial", Font.BOLD, 13));
        formPanel.add(passLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JPasswordField passwordField = new JPasswordField(18);
        passwordField.setFont(UIUtils.FIELD_FONT);
        passwordField.setToolTipText(isEditing ?
            I18n.get("admin.tooltip.passwordEdit") :
            I18n.get("admin.tooltip.passwordNew"));
        UIUtils.applyRoundedBorder(passwordField);
        formPanel.add(passwordField, gbc);

        // Admin checkbox
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        JCheckBox adminCheckBox = new JCheckBox(I18n.get("admin.checkbox.admin"));
        adminCheckBox.setFont(new Font("Arial", Font.BOLD, 13));
        adminCheckBox.setBackground(UIUtils.BG_COLOR);
        adminCheckBox.setToolTipText(I18n.get("admin.tooltip.admin"));
        if (isEditing) {
            adminCheckBox.setSelected(user.isAdmin());
        }
        formPanel.add(adminCheckBox, gbc);

        // Help text
        gbc.gridy = 3;
        JLabel helpLabel = UIUtils.createLabel(isEditing ?
            I18n.get("admin.help.edit") :
            I18n.get("admin.help.add"));
        helpLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        helpLabel.setForeground(new Color(120, 120, 120));
        formPanel.add(helpLabel, gbc);

        dialog.add(formPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(UIUtils.BG_COLOR);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 15, 20));

        JButton cancelButton = UIUtils.createStyledButton(I18n.get("admin.button.cancel"), e -> dialog.dispose(), I18n.get("admin.tooltip.cancel"));
        cancelButton.setBackground(new Color(108, 117, 125));
        buttonPanel.add(cancelButton);

        JButton saveButton = UIUtils.createStyledButton(
            isEditing ? I18n.get("admin.button.save") : I18n.get("admin.button.add"),
            e -> saveUser(dialog, user, usernameField, passwordField, adminCheckBox),
            isEditing ? I18n.get("admin.tooltip.saveEdit") : I18n.get("admin.tooltip.saveAdd"));
        saveButton.setBackground(new Color(40, 167, 69));
        buttonPanel.add(saveButton);

        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(saveButton);

        dialog.setVisible(true);
    }

    private void saveUser(JDialog dialog, User user, JTextField usernameField,
            JPasswordField passwordField, JCheckBox adminCheckBox) {
        if (isProcessing) return;

        String newUsername = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        boolean isAdmin = adminCheckBox.isSelected();

        // Validation
        if (newUsername.isEmpty()) {
            showError(I18n.get("admin.error.usernameEmpty"), dialog);
            usernameField.requestFocus();
            return;
        }

        if (!UserService.validateUsername(newUsername)) {
            showError(I18n.get("admin.error.usernameInvalid"), dialog);
            usernameField.requestFocus();
            return;
        }

        if (user == null && password.isEmpty()) {
            showError(I18n.get("admin.error.passwordRequired"), dialog);
            passwordField.requestFocus();
            return;
        }

        if (!password.isEmpty() && !UserService.validatePassword(password, user == null)) {
            showError(I18n.get("admin.error.passwordInvalid"), dialog);
            passwordField.requestFocus();
            return;
        }

        setProcessing(true);
        statusLabel.setText(I18n.get("admin.status.saving"));

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private String errorMessage = null;

            @Override
            protected Boolean doInBackground() {
                try {
                    if (user == null) {
                        userService.addUser(newUsername, password, isAdmin);
                    } else {
                        userService.editUser(user.getUsername(), newUsername, password, isAdmin);
                    }
                    return true;
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Erro ao salvar usuário", ex);
                    errorMessage = ex.getMessage();
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        String successMsg = user == null ?
                            I18n.get("admin.status.added", newUsername) :
                            I18n.get("admin.status.updated");
                        showMessage(successMsg);
                        loadUsers(null);
                        dialog.dispose();
                    } else {
                        showError(I18n.get("admin.error.saving", errorMessage), dialog);
                    }
                } catch (Exception ex) {
                    showError(I18n.get("admin.error.unexpected", ex.getMessage()), dialog);
                } finally {
                    setProcessing(false);
                }
            }
        };
        worker.execute();
    }

    private void loadUsers(String searchTerm) {
        setProcessing(true);
        statusLabel.setText(I18n.get("admin.status.loading"));
        tableModel.setRowCount(0);

        SwingWorker<List<User>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<User> doInBackground() throws SQLException {
                return userService.getUsers(searchTerm);
            }

            @Override
            protected void done() {
                try {
                    List<User> users = get();
                    for (User user : users) {
                        tableModel.addRow(new Object[]{
                            user.getUsername(),
                            user.isAdmin() ? I18n.get("admin.table.type.admin") : I18n.get("admin.table.type.user"),
                            user.getTimeSinceLastLogin(),
                            user.getAccountCreatedFormatted()
                        });
                    }

                    userCountLabel.setText(I18n.get("admin.count", users.size()));
                    statusLabel.setText(I18n.get("admin.status.loaded", users.size(),
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Erro ao carregar usuários", ex);
                    showError(I18n.get("admin.error.loading", ex.getMessage()));
                } finally {
                    setProcessing(false);
                }
            }
        };
        worker.execute();
    }

    private void deleteUser() {
        if (isProcessing) return;

        User user = getSelectedUser();
        if (user == null) {
            showError(I18n.get("admin.error.selectUserDelete"));
            return;
        }

        if (user.getUsername().equals(username)) {
            showError(I18n.get("admin.error.cannotDeleteSelf"));
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            I18n.get("admin.confirm.deleteMessage", user.getUsername()),
            I18n.get("admin.dialog.confirmDelete"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            statusLabel.setText(I18n.get("admin.status.cancelled"));
            return;
        }

        setProcessing(true);
        statusLabel.setText(I18n.get("admin.status.deleting"));

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    userService.deleteUser(user.getUsername());
                    return true;
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Erro ao excluir usuário", ex);
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        showMessage(I18n.get("admin.status.deleted", user.getUsername()));
                        loadUsers(null);
                    } else {
                        showError(I18n.get("admin.error.deleteUser"));
                    }
                } catch (Exception ex) {
                    showError(I18n.get("admin.error.unexpected", ex.getMessage()));
                } finally {
                    setProcessing(false);
                }
            }
        };
        worker.execute();
    }

    private void searchUsers() {
        String searchTerm = searchField.getText().trim();
        loadUsers(searchTerm.isEmpty() ? null : searchTerm);
    }

    private void clearSearch() {
        searchField.setText("");
        sorter.setRowFilter(null);
        loadUsers(null);
    }

    private void refreshTable() {
        String currentSearch = searchField.getText().trim();
        loadUsers(currentSearch.isEmpty() ? null : currentSearch);
    }

    private void performQuickFilter() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
        }
    }

    private User getSelectedUser() {
        int row = userTable.getSelectedRow();
        if (row == -1) return null;

        int modelRow = userTable.convertRowIndexToModel(row);
        String username = (String) tableModel.getValueAt(modelRow, 0);
        String typeCell = (String) tableModel.getValueAt(modelRow, 1);
        boolean isAdmin = typeCell.contains("Admin");

        return new User(username, "", isAdmin);
    }

    private void setProcessing(boolean processing) {
        isProcessing = processing;
        addButton.setEnabled(!processing);
        editButton.setEnabled(!processing);
        deleteButton.setEnabled(!processing);
        searchButton.setEnabled(!processing);
        clearSearchButton.setEnabled(!processing);
        refreshButton.setEnabled(!processing);
        backButton.setEnabled(!processing);
        searchField.setEnabled(!processing);
        userTable.setEnabled(!processing);
    }

    private void returnToPokedex() {
        if (isProcessing) return;

        dispose();
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Pokédex - " + username);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 800);
            frame.setLocationRelativeTo(null);
            frame.setContentPane(new PokedexPanel(frame, username));
            frame.setVisible(true);
        });
    }

    private void showError(String message, Component parent) {
        JOptionPane.showMessageDialog(parent, message, "Erro", JOptionPane.ERROR_MESSAGE);
        statusLabel.setText("✗ " + message);
        statusLabel.setForeground(UIUtils.ERROR_COLOR);

        Timer timer = new Timer(3000, e -> statusLabel.setForeground(new Color(80, 80, 80)));
        timer.setRepeats(false);
        timer.start();
    }

    private void showError(String message) {
        showError(message, this);
    }

    private void showMessage(String message) {
        statusLabel.setText("✓ " + message);
        statusLabel.setForeground(new Color(40, 167, 69));

        Timer timer = new Timer(3000, e -> statusLabel.setForeground(new Color(80, 80, 80)));
        timer.setRepeats(false);
        timer.start();
    }
}
