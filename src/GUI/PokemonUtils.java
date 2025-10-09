package GUI;

public class PokemonUtils {
    public static boolean isValidId(String text) {
        try {
            int id = Integer.parseInt(text);
            return id >= 1 && id <= 151;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}