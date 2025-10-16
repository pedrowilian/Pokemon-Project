package frontend.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import backend.application.service.PokemonService;
import backend.application.service.UserService;
import backend.domain.model.Pokemon;
import backend.domain.service.IPokemonRepository.AttributeMaxValues;
import backend.infrastructure.ServiceLocator;
import frontend.util.UIUtils;
import shared.util.ReadTextFile;

public class PokedexPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(PokedexPanel.class.getName());
    private static final String IMAGE_DIR = "Images/Image-Pokedex/";
    private final JFrame parentFrame;
    private final String username;
    private final boolean isAdmin;
    private final PokemonService pokemonService;
    private final UserService userService;
    private final AttributeMaxValues maxAttributeValues;

    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField idField;
    private JComboBox<String> typeFilter;
    private JSlider hpSlider, attackSlider, defenseSlider, spAtkSlider, spDefSlider, speedSlider;
    private JLabel hpLabel, attackLabel, defenseLabel, spAtkLabel, spDefLabel, speedLabel, statusBar, adminStatusLabel;
    private JButton voltarButton, adminButton;

    public PokedexPanel(JFrame parentFrame, String username) {
        this.parentFrame = parentFrame;
        this.username = username;
        this.pokemonService = ServiceLocator.getInstance().getPokemonService();
        this.userService = ServiceLocator.getInstance().getUserService();
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

    @SuppressWarnings("UseSpecificCatch")
    private void setLookAndFeel() {
        try {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
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
            ImageIcon pokeballIcon = new ImageIcon("Images/poke-ball.png");
            Image pokeballImage = pokeballIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
            pokeballIcon = new ImageIcon(pokeballImage);
            JLabel pokeballLabel = new JLabel(pokeballIcon);

            // Easter Egg: Triple click on left Poké Ball
            if (isLeft) {
                pokeballLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                pokeballLabel.setToolTipText("???");
                pokeballLabel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 3) {
                            showEasterEgg();
                        }
                    }
                });
            }

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
                SwingUtilities.invokeLater(() -> new AdminFrame(username).setVisible(true));
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
            new SliderConfig("HP", maxAttributeValues.maxHP()),
            new SliderConfig("Attack", maxAttributeValues.maxAttack()),
            new SliderConfig("Defense", maxAttributeValues.maxDefense()),
            new SliderConfig("Sp. Atk", maxAttributeValues.maxSpAtk()),
            new SliderConfig("Sp. Def", maxAttributeValues.maxSpDef()),
            new SliderConfig("Speed", maxAttributeValues.maxSpeed())
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

    private void startBattle() {
        parentFrame.getContentPane().removeAll();
        parentFrame.setContentPane(new TeamSelectionPanel(parentFrame, username));
        parentFrame.revalidate();
        parentFrame.repaint();
    }

    private void addButtons(JPanel panel, GridBagConstraints gbc) {
        ButtonConfig[] buttons = {
            new ButtonConfig("Buscar", e -> buscarPorId(), "Buscar por ID"),
            new ButtonConfig("Mostrar Todos", e -> resetAndShowAll(), "Mostrar todos os Pokémons"),
            new ButtonConfig("Battle", e -> startBattle(), "Iniciar batalha"),
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
                if (c instanceof JLabel jLabel) {
                    jLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    if (col == 0) {
                        jLabel.setToolTipText((String) getValueAt(row, 2));
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
        String name = (String) tableModel.getValueAt(row, 2);
        statusBar.setText("Pokémon selecionado: " + name + " (ID: " + id + ")");
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

        voltarButton = UIUtils.createStyledButton("Voltar", e -> {
            closeConnections();
            parentFrame.dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        }, "Voltar para a tela de login");
        bottomButtonPanel.add(voltarButton);

        bottomPanel.add(bottomButtonPanel, BorderLayout.CENTER);

        return bottomPanel;
    }

    private boolean checkAdmin() {
        if (username == null || username.isEmpty()) {
            return false;
        }
        try {
            return userService.isAdmin(username);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Erro ao verificar status de administrador", ex);
            return false;
        }
    }

    private AttributeMaxValues getMaxAttributeValues() {
        try {
            return pokemonService.getMaxAttributeValues();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Erro ao carregar valores máximos dos atributos", ex);
            showError("Erro ao carregar valores máximos dos atributos.");
            // Return default values in case of error
            return new AttributeMaxValues(255, 200, 250, 200, 230, 200);
        }
    }

    private String[] getPokemonTypes() {
        try {
            return pokemonService.getAllTypes();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Erro ao carregar tipos de Pokémon", ex);
            return new String[]{"All"};
        }
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
        @SuppressWarnings("UnnecessaryTemporaryOnConversionFromString")
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
        hpLabel.setText("HP: 0-" + maxAttributeValues.maxHP());
        attackLabel.setText("Attack: 0-" + maxAttributeValues.maxAttack());
        defenseLabel.setText("Defense: 0-" + maxAttributeValues.maxDefense());
        spAtkLabel.setText("Sp. Atk: 0-" + maxAttributeValues.maxSpAtk());
        spDefLabel.setText("Sp. Def: 0-" + maxAttributeValues.maxSpDef());
        speedLabel.setText("Speed: 0-" + maxAttributeValues.maxSpeed());
        applyFilters();
        statusBar.setText("Campos limpos. Mostrando todos os Pokémons.");
    }

    private void carregarDados(Integer id, String type, int minHp, int minAttack, int minDefense,
                              int minSpAtk, int minSpDef, int minSpeed) {
        tableModel.setRowCount(0);

        try {
            // Convert "All" to null for the service call
            String typeFilter = (type != null && type.equals("All")) ? null : type;

            // Convert 0 values to null (no filter) for the service call
            Integer minHpFilter = minHp > 0 ? minHp : null;
            Integer minAttackFilter = minAttack > 0 ? minAttack : null;
            Integer minDefenseFilter = minDefense > 0 ? minDefense : null;
            Integer minSpAtkFilter = minSpAtk > 0 ? minSpAtk : null;
            Integer minSpDefFilter = minSpDef > 0 ? minSpDef : null;
            Integer minSpeedFilter = minSpeed > 0 ? minSpeed : null;

            // Call the service with filters
            List<Pokemon> pokemons = pokemonService.findWithFilters(
                id,
                typeFilter,
                minHpFilter, null,      // HP min/max
                minAttackFilter, null,  // Attack min/max
                minDefenseFilter, null, // Defense min/max
                minSpAtkFilter, null,   // SpAtk min/max
                minSpDefFilter, null,   // SpDef min/max
                minSpeedFilter, null    // Speed min/max
            );

            // Populate the table with results
            boolean found = false;
            for (Pokemon pokemon : pokemons) {
                found = true;
                Object[] row = {
                    carregarIcon(pokemon.getId()),
                    pokemon.getId(),
                    pokemon.getName(),
                    pokemon.getForm(),
                    pokemon.getType1(),
                    pokemon.getType2(),
                    pokemon.getHp(),
                    pokemon.getAttack(),
                    pokemon.getDefense(),
                    pokemon.getSpAtk(),
                    pokemon.getSpDef(),
                    pokemon.getSpeed(),
                    pokemon.getGeneration()
                };
                tableModel.addRow(row);
            }

            statusBar.setText(found ? "Mostrando Pokémons filtrados." :
                "Nenhum Pokémon encontrado" + (id == null ? "" : " com ID " + id) + ".");

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
        // No database connections to close - using service layer
        LOGGER.log(Level.INFO, "PokedexPanel closed - connections managed by service layer");
    }

    private void validateIdField(JTextField field) {
        field.setBorder(UIUtils.createRoundedBorder(PokemonUtils.isValidId(field.getText().trim()) ? Color.GRAY : UIUtils.ERROR_COLOR));
    }

    private void showError(String message, JTextField... fields) {
        JOptionPane.showMessageDialog(this, message, "Erro", JOptionPane.ERROR_MESSAGE);
        if (statusBar != null) {
            statusBar.setText(message);
        }
        for (JTextField field : fields) {
            if (field != null) {
                field.setBorder(UIUtils.createRoundedBorder(UIUtils.ERROR_COLOR));
            }
        }
    }

    /**
     * Easter Egg: Shows the content of EasterEgg.txt file
     * Activated by triple-clicking the left Poké Ball
     */
    private void showEasterEgg() {
        LOGGER.log(Level.INFO, "Easter Egg activated by user: {0}", username);

        String content = ReadTextFile.readEasterEgg();

        JOptionPane.showMessageDialog(
            this,
            content,
            "Easter Egg - Mensagem Secreta!",
            JOptionPane.INFORMATION_MESSAGE
        );

        statusBar.setText("Easter Egg encontrado! Parabéns, " + username + "!");
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
}
