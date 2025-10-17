package frontend.view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;

import backend.domain.model.Move;
import shared.util.TypeTranslator;

/**
 * UI utility methods for Pokemon-related components
 * Provides styled components and type-based coloring
 */
public class PokemonUtils {

    /**
     * Validate Pokemon ID (Gen 1: 1-151)
     */
    public static boolean isValidId(String text) {
        try {
            int id = Integer.parseInt(text);
            return id >= 1 && id <= 151;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Create a styled type badge (label with background color)
     * Types are automatically translated to the current language
     */
    public static JLabel createTypeBadge(String type) {
        // Translate type to current language
        String translatedType = TypeTranslator.translate(type);
        
        JLabel badge = new JLabel(" " + translatedType.toUpperCase() + " ");
        badge.setFont(new Font("Arial", Font.BOLD, 10));
        badge.setForeground(Color.WHITE);
        badge.setOpaque(true);
        badge.setBackground(getTypeColor(type)); // Use original type for color
        badge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        return badge;
    }

    /**
     * Create a styled attack button for battle UI
     * Types are automatically translated to the current language
     */
    public static JButton createAttackButton(Move move, ActionListener listener) {
        Color typeColor = getTypeColor(move.getType());
        Color disabledColor = new Color(120, 120, 120);
        
        // Translate type to current language
        String translatedType = TypeTranslator.translate(move.getType());

        JButton button = new JButton(String.format(
            "<html><div style='text-align:center'><b>%s</b><br><span style='font-size:8px'>PWR: %d | %s</span></div></html>",
            move.getName(), move.getPower(), translatedType.toUpperCase()));

        button.setFont(new Font("Arial", Font.BOLD, 11));
        button.setBackground(typeColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(typeColor.darker().darker(), 2, true),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(140, 42));
        button.setMinimumSize(new Dimension(100, 36));

        // Store original color as client property for enable/disable
        button.putClientProperty("originalColor", typeColor);
        button.putClientProperty("disabledColor", disabledColor);

        if (listener != null) {
            button.addActionListener(listener);
        }

        // Hover effects
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    Color original = (Color) button.getClientProperty("originalColor");
                    button.setBackground(original.brighter());
                    button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.YELLOW, 3, true),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)
                    ));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    Color original = (Color) button.getClientProperty("originalColor");
                    button.setBackground(original);
                    button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(original.darker().darker(), 3, true),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)
                    ));
                } else {
                    Color disabled = (Color) button.getClientProperty("disabledColor");
                    button.setBackground(disabled);
                }
            }
        });

        return button;
    }

    /**
     * Create a styled action button (Switch, Run, etc.)
     */
    public static JButton createActionButton(String text, Color color) {
        Color disabledColor = new Color(120, 120, 120);

        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 11));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color.darker(), 2, true),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);

        // Store original color as client property
        button.putClientProperty("originalColor", color);
        button.putClientProperty("disabledColor", disabledColor);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    Color original = (Color) button.getClientProperty("originalColor");
                    button.setBackground(original.brighter());
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    Color original = (Color) button.getClientProperty("originalColor");
                    button.setBackground(original);
                } else {
                    Color disabled = (Color) button.getClientProperty("disabledColor");
                    button.setBackground(disabled);
                }
            }
        });

        return button;
    }

    /**
     * Update attack button with new move data
     */
    public static void updateAttackButton(JButton button, Move move) {
        Color typeColor = getTypeColor(move.getType());

        button.setText(String.format(
            "<html><div style='text-align:center'><b>%s</b><br><span style='font-size:9px'>PWR: %d | %s</span></div></html>",
            move.getName(), move.getPower(), move.getType().toUpperCase()));

        // Update the stored original color
        button.putClientProperty("originalColor", typeColor);

        // Only set background if button is enabled
        if (button.isEnabled()) {
            button.setBackground(typeColor);
            button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(typeColor.darker().darker(), 3, true),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
        }
    }

    /**
     * Get color for Pokemon type
     */
    public static Color getTypeColor(String type) {
        return switch (type.toLowerCase()) {
            case "fire" -> new Color(240, 128, 48);
            case "water" -> new Color(104, 144, 240);
            case "electric" -> new Color(248, 208, 48);
            case "grass" -> new Color(120, 200, 80);
            case "ice" -> new Color(152, 216, 216);
            case "fighting" -> new Color(192, 48, 40);
            case "poison" -> new Color(160, 64, 160);
            case "ground" -> new Color(224, 192, 104);
            case "flying" -> new Color(168, 144, 240);
            case "psychic" -> new Color(248, 88, 136);
            case "bug" -> new Color(168, 184, 32);
            case "rock" -> new Color(184, 160, 56);
            case "ghost" -> new Color(112, 88, 152);
            case "dragon" -> new Color(112, 56, 248);
            case "dark" -> new Color(112, 88, 72);
            case "steel" -> new Color(184, 184, 208);
            case "fairy" -> new Color(238, 153, 172);
            default -> new Color(168, 168, 120); // Normal
        };
    }
}
