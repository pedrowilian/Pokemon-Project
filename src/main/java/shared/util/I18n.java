package shared.util;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Internationalization (i18n) manager for the Pokemon application.
 * This class handles loading and retrieving localized strings from resource bundles.
 *
 * ARCHITECTURE NOTE: This class is in shared.util because it's used by both:
 * - Backend (for battle messages, effectiveness text, etc.)
 * - Frontend (for UI labels, buttons, messages, etc.)
 *
 * Supported languages:
 * - Portuguese (Brazil) - pt_BR (default)
 * - English (US) - en_US
 * - Italian (Italy) - it_IT
 * - French (France) - fr_FR
 * - Spanish (Spain) - es_ES
 *
 * Usage:
 * <pre>
 * // Get a translated string
 * String title = I18n.get("pokedex.title");
 *
 * // Get a formatted string with parameters
 * String message = I18n.get("pokemon.selected", "Pikachu", 25);
 *
 * // Change language
 * I18n.setLocale(new Locale("en", "US"));
 * </pre>
 */
public class I18n {
    private static final Logger LOGGER = Logger.getLogger(I18n.class.getName());
    private static final String BUNDLE_NAME = "messages";
    private static Locale currentLocale = Locale.of("en", "US"); // Default to English US
    private static ResourceBundle bundle;

    static {
        loadBundle();
    }

    /**
     * Loads the resource bundle for the current locale
     */
    private static void loadBundle() {
        try {
            bundle = ResourceBundle.getBundle(BUNDLE_NAME, currentLocale);
            LOGGER.log(Level.INFO, "Loaded resource bundle for locale: {0}", currentLocale);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load resource bundle for locale: " + currentLocale, e);
            // Fallback to default locale
            try {
                bundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.of("en", "US"));
                LOGGER.log(Level.WARNING, "Falling back to default locale: en_US");
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to load default resource bundle", ex);
            }
        }
    }

    /**
     * Gets a localized string for the given key
     *
     * @param key the resource key
     * @return the localized string, or the key itself if not found
     */
    public static String get(String key) {
        try {
            if (bundle != null && bundle.containsKey(key)) {
                return bundle.getString(key);
            } else {
                LOGGER.log(Level.WARNING, "Missing translation key: {0}", key);
                return key;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error getting translation for key: " + key, e);
            return key;
        }
    }

    /**
     * Gets a localized string and formats it with the given arguments
     *
     * @param key the resource key
     * @param args the arguments to format the string with
     * @return the formatted localized string
     */
    public static String get(String key, Object... args) {
        String message = get(key);
        try {
            return String.format(message, args);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error formatting message for key: " + key, e);
            return message;
        }
    }

    /**
     * Changes the current locale and reloads the resource bundle
     *
     * @param locale the new locale to use
     */
    public static void setLocale(Locale locale) {
        if (locale == null) {
            LOGGER.log(Level.WARNING, "Attempted to set null locale, ignoring");
            return;
        }
        currentLocale = locale;
        loadBundle();
        LOGGER.log(Level.INFO, "Locale changed to: {0}", locale);
    }

    /**
     * Gets the current locale
     *
     * @return the current locale
     */
    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    /**
     * Gets all available locales for this application
     *
     * @return array of available locales
     */
    public static Locale[] getAvailableLocales() {
        return new Locale[] {
            Locale.of("pt", "BR"),  // Portuguese (Brazil)
            Locale.of("en", "US"),  // English (US)
            Locale.of("it", "IT"),  // Italian (Italy)
            Locale.of("fr", "FR"),  // French (France)
            Locale.of("es", "ES")   // Spanish (Spain)
        };
    }

    /**
     * Gets the display name of a locale in the current locale
     *
     * @param locale the locale to get the display name for
     * @return the display name
     */
    public static String getLocaleDisplayName(Locale locale) {
        return locale.getDisplayName(currentLocale);
    }
}
