package frontend.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.MediaTracker;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Image Cache with lazy loading for Pokemon images
 * 
 * Features:
 * - Asynchronous image loading (non-blocking UI)
 * - Caffeine cache (high performance)
 * - Placeholder while loading
 * - Thread pool for parallel loading
 * - Pre-loading for common Pokemon
 * 
 * Usage:
 * <pre>
 * ImageCache.loadAsync("Images/Front-Pokemon/1.png", pokemonLabel);
 * </pre>
 */
public class ImageCache {
    private static final Logger LOGGER = Logger.getLogger(ImageCache.class.getName());
    
    // Cache configuration
    private static final long CACHE_EXPIRE_HOURS = 2;
    private static final long CACHE_MAX_SIZE = 500; // 500 images in memory
    
    // Thread pool for async loading
    private static final int LOADER_THREADS = 4;
    private static final ExecutorService loaderPool = Executors.newFixedThreadPool(LOADER_THREADS);
    
    // Caffeine cache
    private static final Cache<String, ImageIcon> cache = Caffeine.newBuilder()
        .expireAfterAccess(CACHE_EXPIRE_HOURS, TimeUnit.HOURS)
        .maximumSize(CACHE_MAX_SIZE)
        .recordStats()
        .build();
    
    // Placeholder images
    private static ImageIcon smallPlaceholder;
    private static ImageIcon mediumPlaceholder;
    private static ImageIcon largePlaceholder;
    
    static {
        // Initialize placeholders
        smallPlaceholder = createPlaceholder(50, 50, new Color(220, 220, 220));
        mediumPlaceholder = createPlaceholder(100, 100, new Color(220, 220, 220));
        largePlaceholder = createPlaceholder(150, 150, new Color(220, 220, 220));
        
        LOGGER.log(Level.INFO, "ImageCache initialized (threads={0}, max={1})", 
                   new Object[]{LOADER_THREADS, CACHE_MAX_SIZE});
    }
    
    /**
     * Load image asynchronously with automatic placeholder
     * 
     * @param path Image path relative to project root
     * @param target JLabel to update when loaded
     */
    public static void loadAsync(String path, JLabel target) {
        loadAsync(path, target, mediumPlaceholder);
    }
    
    /**
     * Load image asynchronously with custom placeholder
     * 
     * @param path Image path relative to project root
     * @param target JLabel to update when loaded
     * @param placeholder Placeholder to show while loading
     */
    public static void loadAsync(String path, JLabel target, ImageIcon placeholder) {
        // Check cache first
        ImageIcon cached = cache.getIfPresent(path);
        if (cached != null) {
            SwingUtilities.invokeLater(() -> target.setIcon(cached));
            return;
        }
        
        // Show placeholder immediately
        SwingUtilities.invokeLater(() -> target.setIcon(placeholder));
        
        // Load in background
        loaderPool.submit(() -> {
            try {
                // Check if file exists
                File imageFile = new File(path);
                if (!imageFile.exists()) {
                    LOGGER.log(Level.WARNING, "Image not found: {0}", path);
                    return;
                }
                
                // Load image
                ImageIcon image = new ImageIcon(path);
                
                // Validate image loaded successfully
                if (image.getImageLoadStatus() == MediaTracker.ERRORED) {
                    LOGGER.log(Level.WARNING, "Error loading image: {0}", path);
                    return;
                }
                
                // Cache it
                cache.put(path, image);
                
                // Update UI on EDT
                SwingUtilities.invokeLater(() -> target.setIcon(image));
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to load image: " + path, e);
            }
        });
    }
    
    /**
     * Load image synchronously (blocks until loaded)
     * Use only when you need the image immediately
     * 
     * @param path Image path
     * @return ImageIcon or placeholder if loading fails
     */
    public static ImageIcon loadSync(String path) {
        // Check cache first
        ImageIcon cached = cache.getIfPresent(path);
        if (cached != null) {
            return cached;
        }
        
        try {
            File imageFile = new File(path);
            if (!imageFile.exists()) {
                LOGGER.log(Level.WARNING, "Image not found: {0}", path);
                return mediumPlaceholder;
            }
            
            ImageIcon image = new ImageIcon(path);
            
            if (image.getImageLoadStatus() == MediaTracker.ERRORED) {
                LOGGER.log(Level.WARNING, "Error loading image: {0}", path);
                return mediumPlaceholder;
            }
            
            cache.put(path, image);
            return image;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load image: " + path, e);
            return mediumPlaceholder;
        }
    }
    
    /**
     * Pre-load commonly used images (starter Pokemon, top 25, etc)
     * Call this during app startup to warm up cache
     */
    public static void preloadCommon() {
        loaderPool.submit(() -> {
            LOGGER.info("🔄 Pre-loading common Pokemon images...");
            
            // Top 25 most popular Pokemon
            int[] popularIds = {1, 4, 7, 25, 6, 9, 3, 94, 150, 151, 
                               249, 250, 245, 248, 383, 382, 384, 
                               487, 493, 643, 644, 646, 718, 719, 720};
            
            int loaded = 0;
            for (int id : popularIds) {
                String frontPath = "Images/Front-Pokemon/" + id + ".png";
                String backPath = "Images/Back-Pokemon/" + id + ".png";
                
                if (new File(frontPath).exists()) {
                    cache.put(frontPath, new ImageIcon(frontPath));
                    loaded++;
                }
                
                if (new File(backPath).exists()) {
                    cache.put(backPath, new ImageIcon(backPath));
                    loaded++;
                }
            }
            
            LOGGER.log(Level.INFO, "✅ Pre-loaded {0} images", loaded);
        });
    }
    
    /**
     * Get cache statistics
     */
    public static String getCacheStats() {
        return String.format(
            "ImageCache | Size: %d/%d | Hit Rate: %.2f%% | Hits: %d | Misses: %d",
            cache.estimatedSize(),
            CACHE_MAX_SIZE,
            cache.stats().hitRate() * 100,
            cache.stats().hitCount(),
            cache.stats().missCount()
        );
    }
    
    /**
     * Clear all cached images
     */
    public static void clearCache() {
        cache.invalidateAll();
        LOGGER.info("🗑️ ImageCache cleared");
    }
    
    /**
     * Shutdown loader thread pool (call on app exit)
     */
    public static void shutdown() {
        loaderPool.shutdown();
        try {
            if (!loaderPool.awaitTermination(5, TimeUnit.SECONDS)) {
                loaderPool.shutdownNow();
            }
            LOGGER.info("ImageCache shutdown complete");
        } catch (InterruptedException e) {
            loaderPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Create a placeholder image
     */
    private static ImageIcon createPlaceholder(int width, int height, Color color) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        
        // Background
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        
        // Border
        g.setColor(Color.GRAY);
        g.drawRect(0, 0, width - 1, height - 1);
        
        // "?" text
        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font("Arial", Font.BOLD, width / 3));
        FontMetrics fm = g.getFontMetrics();
        String text = "?";
        int x = (width - fm.stringWidth(text)) / 2;
        int y = ((height - fm.getHeight()) / 2) + fm.getAscent();
        g.drawString(text, x, y);
        
        g.dispose();
        return new ImageIcon(img);
    }
    
    /**
     * Get placeholder by size
     */
    public static ImageIcon getPlaceholder(int size) {
        if (size <= 50) return smallPlaceholder;
        if (size <= 100) return mediumPlaceholder;
        return largePlaceholder;
    }
}
