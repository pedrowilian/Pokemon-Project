package frontend.util;

import java.awt.FlowLayout;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import shared.util.I18n;

/**
 * A reusable component that provides a language selector for the application.
 * This component displays a dropdown with available languages and allows users
 * to switch between them.
 *
 * <p>Usage example:</p>
 * <pre>
 * // Create a language selector
 * LanguageSelectorComponent selector = new LanguageSelectorComponent(locale -> {
 *     // This callback is called when the user changes the language
 *     JOptionPane.showMessageDialog(null,
 *         "Language changed! Please restart the application to see all changes.",
 *         "Language Changed",
 *         JOptionPane.INFORMATION_MESSAGE);
 * });
 *
 * // Add it to your panel
 * topPanel.add(selector.getComponent());
 * </pre>
 */
public class LanguageSelectorComponent {
    private static final Logger LOGGER = Logger.getLogger(LanguageSelectorComponent.class.getName());

    private final JPanel panel;
    private final JComboBox<String> languageComboBox;
    private final Consumer<Locale> onLanguageChange;

    /**
     * Creates a new language selector component
     *
     * @param onLanguageChange callback that is invoked whensrc/main/java/frontend/util/I18n.java the user changes the language
     */
    public LanguageSelectorComponent(Consumer<Locale> onLanguageChange) {
        this.onLanguageChange = onLanguageChange;
        this.panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        this.panel.setOpaque(false);

        // Create label
        JLabel label = UIUtils.createLabel("Language / Idioma:");
        label.setFont(UIUtils.LABEL_FONT);
        panel.add(label);

        // Create combo box with available languages
        languageComboBox = new JComboBox<>();
        Locale[] availableLocales = I18n.getAvailableLocales();
        Locale currentLocale = I18n.getCurrentLocale();
        int currentIndex = 0;

        for (int i = 0; i < availableLocales.length; i++) {
            Locale locale = availableLocales[i];
            String displayName = getLanguageDisplayName(locale);
            languageComboBox.addItem(displayName);

            if (locale.equals(currentLocale)) {
                currentIndex = i;
            }
        }

        languageComboBox.setSelectedIndex(currentIndex);
        languageComboBox.setFont(UIUtils.LABEL_FONT);

        // Add listener
        languageComboBox.addActionListener(e -> {
            int selectedIndex = languageComboBox.getSelectedIndex();
            if (selectedIndex >= 0) {
                Locale newLocale = availableLocales[selectedIndex];
                changeLanguage(newLocale);
            }
        });

        panel.add(languageComboBox);
    }

    /**
     * Gets a user-friendly display name for the locale
     * Shows both the native name and the English name
     *
     * @param locale the locale
     * @return formatted display name
     */
    private String getLanguageDisplayName(Locale locale) {
        String nativeName = locale.getDisplayLanguage(locale);
        String englishName = locale.getDisplayLanguage(Locale.ENGLISH);

        // Capitalize first letter
        nativeName = nativeName.substring(0, 1).toUpperCase() + nativeName.substring(1);

        if (nativeName.equals(englishName)) {
            return nativeName;
        } else {
            return String.format("%s (%s)", nativeName, englishName);
        }
    }

    /**
     * Changes the application language
     *
     * @param newLocale the new locale to use
     */
    private void changeLanguage(Locale newLocale) {
        Locale oldLocale = I18n.getCurrentLocale();

        if (oldLocale.equals(newLocale)) {
            return; // No change
        }

        LOGGER.log(Level.INFO, "Changing language from {0} to {1}",
            new Object[]{oldLocale.getDisplayName(), newLocale.getDisplayName()});

        // Change the locale in I18n
        I18n.setLocale(newLocale);

        // Notify the callback
        if (onLanguageChange != null) {
            try {
                onLanguageChange.accept(newLocale);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error in language change callback", ex);
            }
        }
    }

    /**
     * Gets the panel containing the language selector
     *
     * @return the JPanel component
     */
    public JPanel getComponent() {
        return panel;
    }

    /**
     * Sets whether the component is enabled
     *
     * @param enabled true to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        languageComboBox.setEnabled(enabled);
    }

    /**
     * Gets the currently selected locale
     *
     * @return the selected locale
     */
    public Locale getSelectedLocale() {
        int selectedIndex = languageComboBox.getSelectedIndex();
        if (selectedIndex >= 0) {
            return I18n.getAvailableLocales()[selectedIndex];
        }
        return I18n.getCurrentLocale();
    }
}
