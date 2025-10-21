package backend.infrastructure.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Rate Limiter simples para proteção contra DDoS e brute force
 * 
 * Implementa sliding window rate limiting por IP
 * Thread-safe para uso em servidores multi-threaded
 * 
 * Uso:
 * <pre>
 * if (!RateLimiter.allowRequest(clientIp)) {
 *     socket.close(); // Rejeita conexão
 *     return;
 * }
 * </pre>
 */
public class RateLimiter {
    private static final Logger LOGGER = Logger.getLogger(RateLimiter.class.getName());
    
    // Configuration
    private static final long COOLDOWN_MS = 1000; // 1 segundo entre requisições do mesmo IP
    private static final int MAX_REQUESTS_PER_MINUTE = 30; // Máximo 30 requisições/minuto
    private static final long CLEANUP_INTERVAL_MS = 60000; // Limpa cache a cada 1 minuto
    
    // Estado
    private static final ConcurrentHashMap<String, RequestHistory> requestHistory = new ConcurrentHashMap<>();
    private static long lastCleanup = System.currentTimeMillis();
    
    /**
     * Verifica se uma requisição do IP especificado deve ser permitida
     * 
     * @param ip Endereço IP do cliente
     * @return true se permitido, false se bloqueado
     */
    public static boolean allowRequest(String ip) {
        long now = System.currentTimeMillis();
        
        // Limpa histórico antigo periodicamente
        cleanupIfNeeded(now);
        
        RequestHistory history = requestHistory.computeIfAbsent(ip, k -> new RequestHistory());
        
        synchronized (history) {
            // Verifica cooldown (anti-spam rápido)
            if (now - history.lastRequest < COOLDOWN_MS) {
                LOGGER.log(Level.WARNING, "🚫 Rate limit (cooldown): {0}", ip);
                return false;
            }
            
            // Remove requisições antigas (fora da janela de 1 minuto)
            history.requests.removeIf(timestamp -> now - timestamp > 60000);
            
            // Verifica limite de requisições/minuto
            if (history.requests.size() >= MAX_REQUESTS_PER_MINUTE) {
                LOGGER.log(Level.WARNING, "🚫 Rate limit (max/min): {0} ({1} req)", 
                           new Object[]{ip, history.requests.size()});
                return false;
            }
            
            // Permite requisição
            history.lastRequest = now;
            history.requests.add(now);
            return true;
        }
    }
    
    /**
     * Remove IPs inativos do cache para liberar memória
     */
    private static void cleanupIfNeeded(long now) {
        if (now - lastCleanup > CLEANUP_INTERVAL_MS) {
            synchronized (requestHistory) {
                requestHistory.entrySet().removeIf(entry -> {
                    synchronized (entry.getValue()) {
                        return now - entry.getValue().lastRequest > CLEANUP_INTERVAL_MS;
                    }
                });
                lastCleanup = now;
                LOGGER.log(Level.FINE, "♻️ Rate limiter cleanup: {0} IPs ativos", 
                           requestHistory.size());
            }
        }
    }
    
    /**
     * Limpa todo o histórico (útil para testes)
     */
    public static void reset() {
        requestHistory.clear();
        LOGGER.info("🔄 Rate limiter reset");
    }
    
    /**
     * Obtém estatísticas do rate limiter
     */
    public static String getStats() {
        return String.format("Rate Limiter | Active IPs: %d | Cooldown: %dms | Max/min: %d",
                           requestHistory.size(), COOLDOWN_MS, MAX_REQUESTS_PER_MINUTE);
    }
    
    /**
     * Histórico de requisições por IP
     */
    private static class RequestHistory {
        long lastRequest = 0;
        final java.util.List<Long> requests = new java.util.ArrayList<>();
    }
}
