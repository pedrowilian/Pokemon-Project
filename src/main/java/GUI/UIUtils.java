package GUI;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.Border;

public class UIUtils {
    public static final Color BG_COLOR = new Color(245, 245, 250);
    public static final Color PRIMARY_COLOR = new Color(220, 20, 60);
    public static final Color ACCENT_COLOR = new Color(51, 181, 229);
    public static final Color ERROR_COLOR = new Color(220, 20, 60);
    public static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    public static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 32);
    public static final Font LABEL_FONT = new Font("Arial", Font.PLAIN, 14);
    public static final Font FIELD_FONT = new Font("Arial", Font.PLAIN, 14);
    public static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 14);
    public static final Dimension BUTTON_SIZE = new Dimension(120, 40);

    public static JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(LABEL_FONT);
        label.setForeground(new Color(50, 50, 50));
        return label;
    }

    public static JButton createStyledButton(String text, ActionListener action, String tooltip) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setBackground(PRIMARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            createRoundedBorder(PRIMARY_COLOR),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
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
                    button.setBorder(BorderFactory.createCompoundBorder(
                        createRoundedBorder(ACCENT_COLOR),
                        BorderFactory.createEmptyBorder(8, 15, 8, 15)
                    ));
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(PRIMARY_COLOR);
                    button.setBorder(BorderFactory.createCompoundBorder(
                        createRoundedBorder(PRIMARY_COLOR),
                        BorderFactory.createEmptyBorder(8, 15, 8, 15)
                    ));
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
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(color);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(x + 1, y + 1, width - 3, height - 3, 12, 12);
                g2d.dispose();
            }
            @Override
            public Insets getBorderInsets(Component c) {
                return new Insets(6, 12, 6, 12);
            }
            @Override
            public boolean isBorderOpaque() {
                return false;
            }
        };
    }

    public static void applyRoundedBorder(JTextField field) {
        field.setBorder(BorderFactory.createCompoundBorder(
            createRoundedBorder(new Color(150, 150, 150)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        field.setOpaque(true);
        field.setBackground(Color.WHITE);
    }

    public static void addFocusEffect(JTextField field, FieldValidator validator) {
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    createRoundedBorder(ACCENT_COLOR),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)
                ));
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
            this(new Color(240, 245, 250), new Color(220, 225, 235));
        }

        public GradientPanel(Color startColor, Color endColor) {
            this.startColor = startColor;
            this.endColor = endColor;
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            GradientPaint gp = new GradientPaint(0, 0, startColor, 0, getHeight(), endColor);
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.dispose();
        }
    }
}