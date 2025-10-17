package shared.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for translating Pokémon types between English (database format) 
 * and localized display text.
 * 
 * This class acts as a bridge between:
 * - Storage Layer: Types stored in English in database ("fire", "water", etc.)
 * - Presentation Layer: Types displayed in user's language ("Fogo", "Fuego", etc.)
 * 
 * Architecture Note:
 * - Types are stored in English in the database for consistency
 * - Translation happens only at the presentation layer
 * - Uses I18n class for actual translation lookups
 * 
 * Usage:
 * <pre>
 * // Translate from DB to display
 * String displayType = TypeTranslator.translate("fire"); // "Fuego" in Spanish
 * 
 * // Translate from display back to DB format (for queries)
 * String dbType = TypeTranslator.toEnglish("Fuego"); // "fire"
 * 
 * // Translate array of types
 * String[] displayTypes = TypeTranslator.translateTypes(dbTypes);
 * </pre>
 * 
 * @author Pedro
 * @version 2.0
 */
public class TypeTranslator {
    
    /**
     * All Pokémon types in English (canonical names used in database)
     */
    private static final String[] ALL_TYPES = {
        "normal", "fire", "water", "electric", "grass", "ice", 
        "fighting", "poison", "ground", "flying", "psychic", "bug",
        "rock", "ghost", "dragon", "dark", "steel", "fairy"
    };
    
    /**
     * Cache for reverse translations (localized -> English)
     * Rebuilt when language changes
     */
    private static final Map<String, String> reverseCache = new HashMap<>();
    private static String lastLocale = "";
    
    /**
     * Translates a Pokémon type from English (database format) to localized display text.
     * 
     * @param englishType The type in English (e.g., "fire", "water", "grass")
     * @return Localized type name according to current language
     */
    public static String translate(String englishType) {
        if (englishType == null || englishType.trim().isEmpty()) {
            return "";
        }
        
        // Convert to lowercase to match property keys (database might have "Fire" or "FIRE")
        String normalized = englishType.toLowerCase().trim();
        
        // Build the i18n key: "type.fire", "type.water", etc.
        String key = "type." + normalized;
        
        // Get translated value from I18n
        String translated = I18n.get(key);
        
        // If translation not found, I18n returns the key itself
        // In that case, return the original capitalized type
        if (translated.equals(key)) {
            return capitalize(englishType);
        }
        
        return translated;
    }
    
    /**
     * Translates "All" or any type for use in filter dropdowns.
     * Handles the special case of "All" / "Todos" / "Tous", etc.
     * 
     * @param type The type to translate (can be "All" or a specific type)
     * @return Localized type name
     */
    public static String translateForFilter(String type) {
        if (type == null) {
            return I18n.get("type.all");
        }
        
        // Check if it's "All" in English
        if (type.equalsIgnoreCase("All") || type.trim().isEmpty()) {
            return I18n.get("type.all");
        }
        
        return translate(type);
    }
    
    /**
     * Translates an array of type names from English to localized text.
     * Useful for populating combo boxes and dropdowns.
     * 
     * @param englishTypes Array of type names in English
     * @return Array of localized type names (same order)
     */
    public static String[] translateTypes(String[] englishTypes) {
        if (englishTypes == null || englishTypes.length == 0) {
            return new String[]{I18n.get("type.all")};
        }
        
        String[] translated = new String[englishTypes.length];
        for (int i = 0; i < englishTypes.length; i++) {
            translated[i] = translateForFilter(englishTypes[i]);
        }
        return translated;
    }
    
    /**
     * Converts a localized type name back to English (database format).
     * This is used when user selects a type from a dropdown and we need to query the database.
     * 
     * Example: User selects "Fuego" (Spanish) -> returns "fire" for DB query
     * 
     * @param localizedType The type in user's current language
     * @return The type in English for database queries, or null if "All" is selected
     */
    public static String toEnglish(String localizedType) {
        if (localizedType == null) {
            return null;
        }
        
        // Check if it's "All" in current language
        if (localizedType.equals(I18n.get("type.all"))) {
            return null; // null means no type filter
        }
        
        // Rebuild cache if locale changed
        String currentLocale = I18n.getCurrentLocale().toString();
        if (!currentLocale.equals(lastLocale)) {
            buildReverseCache();
            lastLocale = currentLocale;
        }
        
        // Look up in reverse cache
        String englishType = reverseCache.get(localizedType);
        if (englishType != null) {
            return englishType;
        }
        
        // If not found in cache, assume it's already in English or try to match
        // Check if it matches any English type (case-insensitive)
        for (String type : ALL_TYPES) {
            if (type.equalsIgnoreCase(localizedType)) {
                return capitalize(type);
            }
        }
        
        // Last resort: return as-is, capitalized
        return capitalize(localizedType);
    }
    
    /**
     * Builds a reverse lookup cache: localized name -> English name
     * This is called when locale changes to speed up reverse translations
     */
    private static void buildReverseCache() {
        reverseCache.clear();
        
        for (String englishType : ALL_TYPES) {
            String localizedType = translate(englishType);
            reverseCache.put(localizedType, capitalize(englishType));
        }
    }
    
    /**
     * Capitalizes the first letter of a string (for database format: "Fire", "Water", etc.)
     * 
     * @param str The string to capitalize
     * @return Capitalized string
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
    
    /**
     * Gets all available Pokémon types in English (canonical names).
     * Useful for iteration or validation.
     * 
     * @return Array of all type names in English
     */
    public static String[] getAllTypesInEnglish() {
        String[] types = new String[ALL_TYPES.length];
        for (int i = 0; i < ALL_TYPES.length; i++) {
            types[i] = capitalize(ALL_TYPES[i]);
        }
        return types;
    }
    
    /**
     * Gets all available Pokémon types in current language.
     * Useful for displaying type lists.
     * 
     * @return Array of all type names in current language
     */
    public static String[] getAllTypesLocalized() {
        String[] types = new String[ALL_TYPES.length];
        for (int i = 0; i < ALL_TYPES.length; i++) {
            types[i] = translate(ALL_TYPES[i]);
        }
        return types;
    }
}
