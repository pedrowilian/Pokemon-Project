package shared.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for bidirectional translation of Pokémon move names.
 */
public class MoveTranslator {
    
    // Cache for translations: English -> Current Locale
    private static final Map<String, String> translationCache = new ConcurrentHashMap<>();
    
    // Reverse cache for translations: Localized -> English (if needed)
    private static final Map<String, String> reverseCache = new ConcurrentHashMap<>();
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private MoveTranslator() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }
    
    /**
     * Translates a move name from English to the current locale.
     * Used when displaying move names in the UI.
     * 
     * @param englishName The move name in English (as stored in database)
     * @return The translated move name for display, or original if translation not found
     * 
     * @example
     * <pre>
     * MoveTranslator.translate("Fire Blast")  // Returns "Explosão de Fogo" in pt_BR
     * MoveTranslator.translate("Thunderbolt") // Returns "Raio" in pt_BR
     * </pre>
     */
    public static String translate(String englishName) {
        if (englishName == null || englishName.isEmpty()) {
            return englishName;
        }
        
        // Check cache first for performance
        String cached = translationCache.get(englishName);
        if (cached != null) {
            return cached;
        }
        
        // Convert move name to property key format
        // "Fire Blast" -> "move.fire_blast"
        // "X-Scissor" -> "move.x_scissor"
        String key = "move." + englishName.toLowerCase()
            .replace(" ", "_")
            .replace("-", "_");
        
        // Get translation from resource bundle
        String translated = I18n.get(key);
        
        // If translation not found (returns the key itself), use original English name
        if (translated.equals(key)) {
            translated = englishName;
        }
        
        // Cache both directions
        translationCache.put(englishName, translated);
        reverseCache.put(translated.toLowerCase(), englishName);
        
        return translated;
    }
    
    /**
     * Converts a localized move name back to English.
     * Currently not used in the application, but provided for completeness.
     * Backend always uses English names directly.
     * 
     * @param localizedName The move name in current locale
     * @return The English move name, or original if not found in cache
     * 
     * @example
     * <pre>
     * MoveTranslator.toEnglish("Explosão de Fogo") // Returns "Fire Blast"
     * MoveTranslator.toEnglish("Raio")             // Returns "Thunderbolt"
     * </pre>
     */
    public static String toEnglish(String localizedName) {
        if (localizedName == null || localizedName.isEmpty()) {
            return localizedName;
        }
        
        // Check reverse cache
        String cached = reverseCache.get(localizedName.toLowerCase());
        if (cached != null) {
            return cached;
        }
        
        // If not found in cache, assume it's already in English
        return localizedName;
    }
    
    /**
     * Translates an array of move names from English to current locale.
     * Useful for batch translations.
     * 
     * @param englishMoves Array of move names in English
     * @return Array of translated move names, or null if input is null
     * 
     * @example
     * <pre>
     * String[] moves = {"Fire Blast", "Thunderbolt", "Ice Beam"};
     * String[] translated = MoveTranslator.translateMoves(moves);
     * // In pt_BR: ["Explosão de Fogo", "Raio", "Raio de Gelo"]
     * </pre>
     */
    public static String[] translateMoves(String[] englishMoves) {
        if (englishMoves == null) {
            return null;
        }
        
        String[] translated = new String[englishMoves.length];
        for (int i = 0; i < englishMoves.length; i++) {
            translated[i] = translate(englishMoves[i]);
        }
        return translated;
    }
    
    /**
     * Clears all translation caches.
     * Should be called when the application locale changes to ensure
     * fresh translations are loaded.
     * 
     * @implNote Called by I18n.setLocale() when user changes language
     */
    public static void clearCache() {
        translationCache.clear();
        reverseCache.clear();
    }
    
    /**
     * Gets all cached translations for debugging purposes.
     * 
     * @return Immutable copy of the translation cache
     */
    public static Map<String, String> getCachedTranslations() {
        return new HashMap<>(translationCache);
    }
    
    /**
     * Gets the number of cached translations.
     * Useful for monitoring and debugging.
     * 
     * @return Number of moves currently in translation cache
     */
    public static int getCacheSize() {
        return translationCache.size();
    }
}
