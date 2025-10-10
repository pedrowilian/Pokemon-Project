package GUI;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import model.Pokemon;

public class PokedexPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(PokedexPanel.class.getName());
    private static final String IMAGE_DIR = "Images/Image-Pokedex/";
    private final Connection pokedexConn;
    private final Connection usuariosConn;
    private final JFrame parentFrame;
    private final String username;
    private final boolean isAdmin;
    private final AttributeMaxValues maxAttributeValues;

    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField idField;
    private JComboBox<String> typeFilter;
    private JSlider hpSlider, attackSlider, defenseSlider, spAtkSlider, spDefSlider, speedSlider;
    private JLabel hpLabel, attackLabel, defenseLabel, spAtkLabel, spDefLabel, speedLabel, statusBar, adminStatusLabel;
    private JButton buscarButton, mostrarTodosButton, clearButton, sairButton, voltarButton, adminButton, battleButton;
    private Pokemon selectedPokemon;

    public PokedexPanel(Connection pokedexConn, Connection usuariosConn, JFrame parentFrame, String username) {
        this.pokedexConn = pokedexConn;
        this.usuariosConn = usuariosConn;
        this.parentFrame = parentFrame;
        this.username = username;
        this.isAdmin = checkAdmin();
        this.maxAttributeValues = getMaxAttributeValues();

        initializeUI();
        carregarDados(null, null, 0, 0, 0, 0, 0, 0);
    }

    private void initializeUI() {
        setLookAndFeel();
        configureFrame();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(UIUtils.BG_COLOR);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                repaint();
            }
        });

        add(createControlPanel(), BorderLayout.NORTH);
        add(createTablePane(), BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);
    }

    private void setLookAndFeel() {
        try {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
        e.printStackTrace();
        }
    }

    private void configureFrame() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        parentFrame.setSize((int) (screenSize.width * 0.9), (int) (screenSize.height * 0.9));
        parentFrame.setLocationRelativeTo(null);
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new UIUtils.GradientPanel();
        controlPanel.setLayout(new GridBagLayout());
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIUtils.PRIMARY_COLOR, 2, true),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.NONE;

        // Add Poké Ball images
        addPokeballImage(controlPanel, gbc, 0, true);
        addPokeballImage(controlPanel, gbc, 5, false);

        // Top right panel (Admin status and button)
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        gbc.gridheight = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.8;
        controlPanel.add(createTopRightPanel(), gbc);

        // ID field and label
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.gridy = 1;
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.2;
        controlPanel.add(UIUtils.createLabel("ID:"), gbc);

        gbc.gridx = 2;
        idField = createIdField();
        controlPanel.add(idField, gbc);

        // Type filter and label
        gbc.gridx = 3;
        controlPanel.add(UIUtils.createLabel("Type:"), gbc);

        gbc.gridx = 4;
        typeFilter = createTypeFilter();
        controlPanel.add(typeFilter, gbc);

        // Add sliders
        addSliders(controlPanel, gbc);

        // Add buttons
        addButtons(controlPanel, gbc);

        return controlPanel;
    }

    private void addPokeballImage(JPanel panel, GridBagConstraints gbc, int gridx, boolean isLeft) {
        gbc.gridx = gridx;
        gbc.gridy = 0;
        gbc.gridheight = 5;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 0.1;
        try {
            ImageIcon pokeballIcon = new ImageIcon("poke-ball.png");
            Image pokeballImage = pokeballIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
            pokeballIcon = new ImageIcon(pokeballImage);
            JLabel pokeballLabel = new JLabel(pokeballIcon);
            panel.add(pokeballLabel, gbc);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro ao carregar a imagem poke-ball.png (" + (isLeft ? "esquerda" : "direita") + ")", e);
            showError("Erro ao carregar a imagem Poké Ball (" + (isLeft ? "esquerda" : "direita") + "). Verifique se o arquivo 'poke-ball.png' está no diretório do projeto.");
        }
    }

    private JPanel createTopRightPanel() {
        JPanel topRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topRightPanel.setOpaque(false);
        adminStatusLabel = UIUtils.createLabel("Modo: " + (isAdmin ? "Admin" : "Usuário"));
        topRightPanel.add(adminStatusLabel);

        if (isAdmin) {
            adminButton = UIUtils.createStyledButton("Usuários", e -> {
                parentFrame.dispose();
                SwingUtilities.invokeLater(() -> new AdminFrame(pokedexConn, usuariosConn, username).setVisible(true));
            }, "Acessar painel de administração");
            topRightPanel.add(adminButton);
        }
        return topRightPanel;
    }

    private JTextField createIdField() {
        JTextField field = new JTextField(5);
        field.setFont(UIUtils.LABEL_FONT);
        field.setToolTipText("Digite o ID do Pokémon (1-151)");
        UIUtils.applyRoundedBorder(field);
        UIUtils.addFocusEffect(field, this::validateIdField);
        field.addActionListener(e -> buscarPorId());
        return field;
    }

    private JComboBox<String> createTypeFilter() {
        JComboBox<String> comboBox = new JComboBox<>(getPokemonTypes());
        comboBox.setFont(UIUtils.LABEL_FONT);
        comboBox.setMaximumRowCount(10);
        comboBox.setToolTipText("Selecione um tipo para filtrar");
        comboBox.addActionListener(e -> applyFilters());
        return comboBox;
    }

    private void addSliders(JPanel panel, GridBagConstraints gbc) {
        SliderConfig[] configs = {
            new SliderConfig("HP", maxAttributeValues.hp()),
            new SliderConfig("Attack", maxAttributeValues.attack()),
            new SliderConfig("Defense", maxAttributeValues.defense()),
            new SliderConfig("Sp. Atk", maxAttributeValues.spAtk()),
            new SliderConfig("Sp. Def", maxAttributeValues.spDef()),
            new SliderConfig("Speed", maxAttributeValues.speed())
        };

        int y = 2;
        for (int i = 0; i < configs.length; i += 2) {
            gbc.gridy = y++;
            gbc.gridx = 1;
            gbc.weightx = 0.2;
            configs[i].label = UIUtils.createLabel(configs[i].name + ": 0-" + configs[i].maxValue);
            panel.add(configs[i].label, gbc);

            gbc.gridx = 2;
            configs[i].slider = createSlider(configs[i].maxValue, configs[i].name);
            panel.add(configs[i].slider, gbc);

            if (i + 1 < configs.length) {
                gbc.gridx = 3;
                configs[i + 1].label = UIUtils.createLabel(configs[i + 1].name + ": 0-" + configs[i + 1].maxValue);
                panel.add(configs[i + 1].label, gbc);

                gbc.gridx = 4;
                configs[i + 1].slider = createSlider(configs[i + 1].maxValue, configs[i + 1].name);
                panel.add(configs[i + 1].slider, gbc);
            }
        }

        hpSlider = configs[0].slider;
        attackSlider = configs[1].slider;
        defenseSlider = configs[2].slider;
        spAtkSlider = configs[3].slider;
        spDefSlider = configs[4].slider;
        speedSlider = configs[5].slider;
        hpLabel = configs[0].label;
        attackLabel = configs[1].label;
        defenseLabel = configs[2].label;
        spAtkLabel = configs[3].label;
        spDefLabel = configs[4].label;
        speedLabel = configs[5].label;
    }

    private JSlider createSlider(int maxValue, String name) {
        JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, maxValue, 0);
        slider.setMajorTickSpacing(Math.max(10, maxValue / 5));
        slider.setMinorTickSpacing(Math.max(2, maxValue / 25));
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setBackground(UIUtils.BG_COLOR);
        slider.setToolTipText("Filtrar por " + name + " mínimo");
        slider.setPreferredSize(new Dimension(150, 40));
        slider.addChangeListener(e -> {
            JSlider source = (JSlider) e.getSource();
            JLabel label = name.equals("HP") ? hpLabel :
                          name.equals("Attack") ? attackLabel :
                          name.equals("Defense") ? defenseLabel :
                          name.equals("Sp. Atk") ? spAtkLabel :
                          name.equals("Sp. Def") ? spDefLabel : speedLabel;
            label.setText(name + ": " + source.getValue() + "-" + maxValue);
            applyFilters();
        });
        return slider;
    }

    private void addButtons(JPanel panel, GridBagConstraints gbc) {
        ButtonConfig[] buttons = {
            new ButtonConfig("Buscar", e -> buscarPorId(), "Buscar por ID"),
            new ButtonConfig("Mostrar Todos", e -> resetAndShowAll(), "Mostrar todos os Pokémons"),
            new ButtonConfig("Limpar", e -> resetAndShowAll(), "Limpar todos os filtros"),
            new ButtonConfig("Sair", e -> {
                closeConnections();
                parentFrame.dispose();
                System.exit(0);
            }, "Sair do aplicativo")
        };

        gbc.gridy = 5;
        gbc.insets = new Insets(10, 5, 5, 5);
        for (int i = 0; i < buttons.length; i++) {
            gbc.gridx = i + 1;
            gbc.weightx = 0.2;
            JButton button = UIUtils.createStyledButton(buttons[i].name, buttons[i].action, buttons[i].tooltip);
            panel.add(button, gbc);
            if (buttons[i].name.equals("Buscar")) buscarButton = button;
            else if (buttons[i].name.equals("Mostrar Todos")) mostrarTodosButton = button;
            else if (buttons[i].name.equals("Limpar")) clearButton = button;
            else if (buttons[i].name.equals("Sair")) sairButton = button;
        }
    }

    private JScrollPane createTablePane() {
        String[] columns = {"Imagem", "ID", "Name", "Form", "Type1", "Type2", "HP", "Attack", "Defense", "SpAtk", "SpDef", "Speed", "Gen"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int col) {
                return col == 0 ? ImageIcon.class : col == 1 || col >= 6 ? Integer.class : String.class;
            }
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                if (c instanceof JLabel) {
                    ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                    if (col == 0) {
                        ((JLabel) c).setToolTipText((String) getValueAt(row, 2));
                    }
                }
                c.setBackground(isRowSelected(row) ? UIUtils.ACCENT_COLOR : row % 2 == 0 ? Color.WHITE : new Color(240, 240, 250));
                return c;
            }
        };
        table.setRowHeight(64);
        table.setFont(UIUtils.LABEL_FONT);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(0).setMinWidth(70);
        table.getColumnModel().getColumn(0).setMaxWidth(100);
        table.getColumnModel().getColumn(12).setPreferredWidth(50);
        table.getTableHeader().setFont(UIUtils.LABEL_FONT);
        table.getTableHeader().setBackground(UIUtils.PRIMARY_COLOR);
        table.getTableHeader().setForeground(Color.WHITE);

        DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                table.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                table.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    selectPokemon();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIUtils.PRIMARY_COLOR, 2, true));
        return scrollPane;
    }

    private void selectPokemon() {
        int row = table.getSelectedRow();
        if (row == -1) return;
        
        int id = (int) tableModel.getValueAt(row, 1);
        selectedPokemon = getPokemonById(id);
        
        if (selectedPokemon != null) {
            statusBar.setText("Pokémon selecionado: " + selectedPokemon.getName() + " (ID: " + id + ")");
            battleButton.setEnabled(true);
        }
    }

    private Pokemon getPokemonById(int id) {
        String sql = "SELECT * FROM pokedex WHERE ID = ?";
        try (PreparedStatement ps = pokedexConn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Pokemon(
                        rs.getInt("ID"),
                        rs.getString("Name"),
                        rs.getString("Form"),
                        rs.getString("Type1"),
                        rs.getString("Type2"),
                        rs.getInt("Total"),
                        rs.getInt("HP"),
                        rs.getInt("Attack"),
                        rs.getInt("Defense"),
                        rs.getInt("SpAtk"),
                        rs.getInt("SpDef"),
                        rs.getInt("Speed"),
                        1
                    );
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar Pokémon", ex);
        }
        return null;
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(UIUtils.BG_COLOR);

        statusBar = UIUtils.createLabel("Pronto");
        statusBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIUtils.PRIMARY_COLOR, 2, true),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        bottomPanel.add(statusBar, BorderLayout.NORTH);

        JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomButtonPanel.setBackground(UIUtils.BG_COLOR);
        
        battleButton = UIUtils.createStyledButton("Batalhar", e -> startBattle(), "Iniciar batalha com Pokémon selecionado");
        battleButton.setEnabled(false);
        bottomButtonPanel.add(battleButton);
        
        voltarButton = UIUtils.createStyledButton("Voltar", e -> {
            closeConnections();
            parentFrame.dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        }, "Voltar para a tela de login");
        bottomButtonPanel.add(voltarButton);
        bottomPanel.add(bottomButtonPanel, BorderLayout.CENTER);

        return bottomPanel;
    }

    private void startBattle() {
        if (selectedPokemon == null) {
            showError("Selecione um Pokémon para batalhar.");
            return;
        }
        
        // Get random enemy pokemon
        Pokemon enemy = getRandomPokemon();
        if (enemy == null) {
            showError("Erro ao selecionar Pokémon inimigo.");
            return;
        }
        
        // Open battle window
        parentFrame.dispose();
        SwingUtilities.invokeLater(() -> {
            JFrame battleFrame = new JFrame("Batalha Pokémon");
            battleFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            battleFrame.setSize(900, 600);
            battleFrame.setLocationRelativeTo(null);
            battleFrame.setContentPane(new BattlePanel(selectedPokemon, enemy, pokedexConn, usuariosConn, username));
            battleFrame.setVisible(true);
        });
    }

    private Pokemon getRandomPokemon() {
        String sql = "SELECT * FROM pokedex ORDER BY RANDOM() LIMIT 1";
        try (Statement stmt = pokedexConn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return new Pokemon(
                    rs.getInt("ID"),
                    rs.getString("Name"),
                    rs.getString("Form"),
                    rs.getString("Type1"),
                    rs.getString("Type2"),
                    rs.getInt("Total"),
                    rs.getInt("HP"),
                    rs.getInt("Attack"),
                    rs.getInt("Defense"),
                    rs.getInt("SpAtk"),
                    rs.getInt("SpDef"),
                    rs.getInt("Speed"),
                    1
                );
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar Pokémon aleatório", ex);
        }
        return null;
    }

    private boolean checkAdmin() {
        if (usuariosConn == null || username == null || username.isEmpty()) {
            showError("Usuário ou conexão inválida.");
            return false;
        }
        String sql = "SELECT admin FROM usuarios WHERE nome = ?";
        try (PreparedStatement ps = usuariosConn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("admin") == 1;
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Erro ao verificar status de administrador", ex);
            showError("Erro ao verificar status de administrador.");
            return false;
        }
    }

    private AttributeMaxValues getMaxAttributeValues() {
        int hp = 255, attack = 200, defense = 250, spAtk = 200, spDef = 230, speed = 200;
        if (pokedexConn == null) {
            return new AttributeMaxValues(hp, attack, defense, spAtk, spDef, speed);
        }
        String sql = "SELECT MAX(HP) as maxHP, MAX(Attack) as maxAttack, MAX(Defense) as maxDefense, " +
                     "MAX(SpAtk) as maxSpAtk, MAX(SpDef) as maxSpDef, MAX(Speed) as maxSpeed FROM pokedex";
        try (Statement stmt = pokedexConn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                hp = rs.getInt("maxHP");
                attack = rs.getInt("maxAttack");
                defense = rs.getInt("maxDefense");
                spAtk = rs.getInt("maxSpAtk");
                spDef = rs.getInt("maxSpDef");
                speed = rs.getInt("maxSpeed");
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Erro ao carregar valores máximos dos atributos", ex);
            showError("Erro ao carregar valores máximos dos atributos.");
        }
        return new AttributeMaxValues(hp, attack, defense, spAtk, spDef, speed);
    }

    private String[] getPokemonTypes() {
        Set<String> types = new HashSet<>();
        types.add("All");
        String sql = "SELECT DISTINCT Type1 FROM pokedex UNION SELECT DISTINCT Type2 FROM pokedex WHERE Type2 IS NOT NULL";
        try (Statement stmt = pokedexConn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                types.add(rs.getString(1));
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Erro ao carregar tipos de Pokémon", ex);
        }
        return types.toArray(new String[0]);
    }

    private void buscarPorId() {
        String txt = idField.getText().trim();
        if (txt.isEmpty()) {
            showError("Por favor, digite um ID.", idField);
            return;
        }
        if (!PokemonUtils.isValidId(txt)) {
            showError("ID deve estar entre 1 e 151.", idField);
            return;
        }
        int id = Integer.parseInt(txt);
        carregarDados(id, (String) typeFilter.getSelectedItem(),
            hpSlider.getValue(), attackSlider.getValue(), defenseSlider.getValue(),
            spAtkSlider.getValue(), spDefSlider.getValue(), speedSlider.getValue());
        statusBar.setText("Mostrando Pokémon com ID " + id);
        idField.setBorder(UIUtils.createRoundedBorder(Color.GRAY));
    }

    private void applyFilters() {
        String type = (String) typeFilter.getSelectedItem();
        String idText = idField.getText().trim();
        Integer id = idText.isEmpty() ? null : PokemonUtils.isValidId(idText) ? Integer.parseInt(idText) : null;
        if (idText.isEmpty() || id != null) {
            carregarDados(id, type, hpSlider.getValue(), attackSlider.getValue(),
                defenseSlider.getValue(), spAtkSlider.getValue(), spDefSlider.getValue(),
                speedSlider.getValue());
            StringBuilder status = new StringBuilder("Filtrando por ");
            status.append("Type: ").append(type.equals("All") ? "All" : type).append(", ");
            status.append("HP >= ").append(hpSlider.getValue()).append(", ");
            status.append("Attack >= ").append(attackSlider.getValue()).append(", ");
            status.append("Defense >= ").append(defenseSlider.getValue()).append(", ");
            status.append("Sp. Atk >= ").append(spAtkSlider.getValue()).append(", ");
            status.append("Sp. Def >= ").append(spDefSlider.getValue()).append(", ");
            status.append("Speed >= ").append(speedSlider.getValue());
            statusBar.setText(status.toString());
        } else {
            showError("ID inválido.", idField);
        }
    }

    private void resetAndShowAll() {
        idField.setText("");
        typeFilter.setSelectedIndex(0);
        hpSlider.setValue(0);
        attackSlider.setValue(0);
        defenseSlider.setValue(0);
        spAtkSlider.setValue(0);
        spDefSlider.setValue(0);
        speedSlider.setValue(0);
        hpLabel.setText("HP: 0-" + maxAttributeValues.hp());
        attackLabel.setText("Attack: 0-" + maxAttributeValues.attack());
        defenseLabel.setText("Defense: 0-" + maxAttributeValues.defense());
        spAtkLabel.setText("Sp. Atk: 0-" + maxAttributeValues.spAtk());
        spDefLabel.setText("Sp. Def: 0-" + maxAttributeValues.spDef());
        speedLabel.setText("Speed: 0-" + maxAttributeValues.speed());
        applyFilters();
        statusBar.setText("Campos limpos. Mostrando todos os Pokémons.");
    }

    private void carregarDados(Integer id, String type, int minHp, int minAttack, int minDefense,
                              int minSpAtk, int minSpDef, int minSpeed) {
        if (pokedexConn == null) {
            showError("Sem conexão com o banco de dados.");
            return;
        }
        StringBuilder sql = new StringBuilder(
            "SELECT ID, Name, Form, Type1, Type2, HP, Attack, Defense, SpAtk, SpDef, Speed FROM pokedex WHERE 1=1"
        );
        int paramCount = 0;
        if (id != null) {
            sql.append(" AND ID = ?");
            paramCount++;
        }
        if (type != null && !type.equals("All")) {
            sql.append(" AND (Type1 = ? OR Type2 = ?)");
            paramCount += 2;
        }
        if (minHp > 0) {
            sql.append(" AND HP >= ?");
            paramCount++;
        }
        if (minAttack > 0) {
            sql.append(" AND Attack >= ?");
            paramCount++;
        }
        if (minDefense > 0) {
            sql.append(" AND Defense >= ?");
            paramCount++;
        }
        if (minSpAtk > 0) {
            sql.append(" AND SpAtk >= ?");
            paramCount++;
        }
        if (minSpDef > 0) {
            sql.append(" AND SpDef >= ?");
            paramCount++;
        }
        if (minSpeed > 0) {
            sql.append(" AND Speed >= ?");
            paramCount++;
        }
        sql.append(" ORDER BY ID");

        tableModel.setRowCount(0);
        try (PreparedStatement ps = pokedexConn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (id != null) {
                ps.setInt(paramIndex++, id);
            }
            if (type != null && !type.equals("All")) {
                ps.setString(paramIndex++, type);
                ps.setString(paramIndex++, type);
            }
            if (minHp > 0) {
                ps.setInt(paramIndex++, minHp);
            }
            if (minAttack > 0) {
                ps.setInt(paramIndex++, minAttack);
            }
            if (minDefense > 0) {
                ps.setInt(paramIndex++, minDefense);
            }
            if (minSpAtk > 0) {
                ps.setInt(paramIndex++, minSpAtk);
            }
            if (minSpDef > 0) {
                ps.setInt(paramIndex++, minSpDef);
            }
            if (minSpeed > 0) {
                ps.setInt(paramIndex++, minSpeed);
            }
            try (ResultSet rs = ps.executeQuery()) {
                boolean achou = false;
                while (rs.next()) {
                    achou = true;
                    int pid = rs.getInt("ID");
                    Object[] row = {
                        carregarIcon(pid),
                        pid,
                        rs.getString("Name"),
                        rs.getString("Form"),
                        rs.getString("Type1"),
                        rs.getString("Type2"),
                        rs.getInt("HP"),
                        rs.getInt("Attack"),
                        rs.getInt("Defense"),
                        rs.getInt("SpAtk"),
                        rs.getInt("SpDef"),
                        rs.getInt("Speed"),
                        1
                    };
                    tableModel.addRow(row);
                }
                statusBar.setText(achou ? "Mostrando Pokémons filtrados." :
                    "Nenhum Pokémon encontrado" + (id == null ? "" : " com ID " + id) + ".");
            }
        } catch (SQLException ex) {
            showError("Erro ao carregar dados: " + ex.getMessage());
            LOGGER.log(Level.SEVERE, "Erro ao carregar dados da Pokédex", ex);
        }
    }

    private ImageIcon carregarIcon(int id) {
        String file = IMAGE_DIR + id + ".png";
        File f = new File(file);
        if (!f.exists()) {
            BufferedImage placeholder = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = placeholder.createGraphics();
            g2d.setColor(Color.GRAY);
            g2d.fillRect(0, 0, 64, 64);
            g2d.setColor(Color.WHITE);
            g2d.setFont(UIUtils.LABEL_FONT);
            g2d.drawString("No Image", 10, 32);
            g2d.dispose();
            return new ImageIcon(placeholder);
        }
        ImageIcon ic = new ImageIcon(file);
        Image img = ic.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    private void closeConnections() {
        try {
            if (pokedexConn != null && !pokedexConn.isClosed()) pokedexConn.close();
            if (usuariosConn != null && !usuariosConn.isClosed()) usuariosConn.close();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Erro ao fechar conexões", ex);
        }
    }

    private void validateIdField(JTextField field) {
        field.setBorder(UIUtils.createRoundedBorder(PokemonUtils.isValidId(field.getText().trim()) ? Color.GRAY : UIUtils.ERROR_COLOR));
    }

    private void showError(String message, JTextField... fields) {
        JOptionPane.showMessageDialog(this, message, "Erro", JOptionPane.ERROR_MESSAGE);
        statusBar.setText(message);
        for (JTextField field : fields) {
            if (field != null) {
                field.setBorder(UIUtils.createRoundedBorder(UIUtils.ERROR_COLOR));
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GradientPaint gp = new GradientPaint(0, 0, UIUtils.BG_COLOR, 0, getHeight(), new Color(200, 200, 200));
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, getWidth(), getHeight());
    }

    private static class SliderConfig {
        String name;
        JSlider slider;
        JLabel label;
        int maxValue;

        SliderConfig(String name, int maxValue) {
            this.name = name;
            this.maxValue = maxValue;
        }
    }

    private static class ButtonConfig {
        String name;
        ActionListener action;
        String tooltip;

        ButtonConfig(String name, ActionListener action, String tooltip) {
            this.name = name;
            this.action = action;
            this.tooltip = tooltip;
        }
    }

    private record AttributeMaxValues(int hp, int attack, int defense, int spAtk, int spDef, int speed) {}

}