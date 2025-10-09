package GUI;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.Border;

public class UIUtils {
    public static final Color BG_COLOR = new Color(240, 240, 240);
    public static final Color PRIMARY_COLOR = new Color(200, 0, 0);
    public static final Color ACCENT_COLOR = new Color(51, 181, 229);
    public static final Color ERROR_COLOR = new Color(220, 20, 60);
    public static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 28);
    public static final Font LABEL_FONT = new Font("Arial", Font.PLAIN, 14);
    public static final Font FIELD_FONT = new Font("Arial", Font.PLAIN, 14);
    public static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 14);
    public static final Dimension BUTTON_SIZE = new Dimension(100, 36);

    public static JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(LABEL_FONT);
        label.setForeground(Color.BLACK);
        return label;
    }

    public static JButton createStyledButton(String text, ActionListener action, String tooltip) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setBackground(PRIMARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(createRoundedBorder(PRIMARY_COLOR));
        button.setPreferredSize(BUTTON_SIZE);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.addActionListener(action);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(ACCENT_COLOR);
                    button.setBorder(createRoundedBorder(ACCENT_COLOR));
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(PRIMARY_COLOR);
                    button.setBorder(createRoundedBorder(PRIMARY_COLOR));
                }
            }
        });
        button.setToolTipText(tooltip);
        return button;
    }

    public static Border createRoundedBorder(Color color) {
        return new Border() {
            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(color);
                g2d.drawRoundRect(x, y, width - 1, height - 1, 10, 10);
            }
            @Override
            public Insets getBorderInsets(Component c) {
                return new Insets(5, 10, 5, 10);
            }
            @Override
            public boolean isBorderOpaque() {
                return false;
            }
        };
    }

    public static void applyRoundedBorder(JTextField field) {
        field.setBorder(createRoundedBorder(Color.GRAY));
        field.setOpaque(false);
    }

    public static void addFocusEffect(JTextField field, FieldValidator validator) {
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(createRoundedBorder(ACCENT_COLOR));
            }
            @Override
            public void focusLost(FocusEvent e) {
                validator.validate(field);
            }
        });
    }

    @FunctionalInterface
    public interface FieldValidator {
        void validate(JTextField field);
    }

    public static class GradientPanel extends JPanel {
        private final Color startColor;
        private final Color endColor;

        public GradientPanel() {
            this(BG_COLOR, new Color(200, 200, 200));
        }

        public GradientPanel(Color startColor, Color endColor) {
            this.startColor = startColor;
            this.endColor = endColor;
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint gp = new GradientPaint(0, 0, startColor, 0, getHeight(), endColor);
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}