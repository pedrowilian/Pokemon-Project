package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReadTextFile {
    private static final Logger LOGGER = Logger.getLogger(ReadTextFile.class.getName());
    private static final String EASTER_EGG_FILE = "EasterEgg.txt";

    /**
     * Reads the Easter Egg file and returns its content
     * @return The content of the Easter Egg file, or an error message if file cannot be read
     */
    public static String readEasterEgg() {
        File file = new File(EASTER_EGG_FILE);

        if (!file.exists()) {
            LOGGER.log(Level.WARNING, "Easter Egg file not found: " + EASTER_EGG_FILE);
            return "Easter Egg file not found!";
        }

        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            LOGGER.log(Level.INFO, "Easter Egg file read successfully");
            return content.toString().trim();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading Easter Egg file", e);
            return "Error reading Easter Egg file: " + e.getMessage();
        }
    }

    /**
     * Checks if the Easter Egg file exists
     * @return true if the file exists, false otherwise
     */
    public static boolean easterEggExists() {
        return new File(EASTER_EGG_FILE).exists();
    }
}
