package shared.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilitários de rede para descobrir IPs e testar conectividade
 */
public class NetworkUtils {
    private static final Logger LOGGER = Logger.getLogger(NetworkUtils.class.getName());
    
    /**
     * Obtém o IP local da máquina (LAN)
     * 
     * @return IP local (ex: 192.168.1.100) ou "localhost"
     */
    public static String getLocalIP() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            return localHost.getHostAddress();
        } catch (UnknownHostException e) {
            LOGGER.log(Level.WARNING, "Erro ao obter IP local", e);
            return "localhost";
        }
    }
    
    /**
     * Obtém o IP público da máquina (Internet)
     * Faz requisição para serviço externo
     * 
     * @return IP público (ex: 177.45.123.89) ou null se falhar
     */
    public static String getPublicIP() {
        String[] services = {
            "https://api.ipify.org",
            "https://ifconfig.me/ip",
            "https://icanhazip.com"
        };
        
        for (String service : services) {
            try {
                URL url = new URL(service);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    String ip = reader.readLine().trim();
                    if (isValidIP(ip)) {
                        LOGGER.log(Level.INFO, "IP público detectado: {0}", ip);
                        return ip;
                    }
                }
            } catch (Exception e) {
                // Tenta próximo serviço
                LOGGER.log(Level.FINE, "Falha ao obter IP de " + service, e);
            }
        }
        
        LOGGER.warning("Não foi possível obter IP público");
        return null;
    }
    
    /**
     * Verifica se uma string é um IP válido
     */
    public static boolean isValidIP(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        
        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Verifica se um IP é privado (LAN) ou público (Internet)
     */
    public static boolean isPrivateIP(String ip) {
        if (!isValidIP(ip)) {
            return false;
        }
        
        // IPs privados (RFC 1918):
        // 10.0.0.0 - 10.255.255.255
        // 172.16.0.0 - 172.31.255.255
        // 192.168.0.0 - 192.168.255.255
        
        String[] parts = ip.split("\\.");
        int first = Integer.parseInt(parts[0]);
        int second = Integer.parseInt(parts[1]);
        
        if (first == 10) {
            return true;
        }
        if (first == 172 && second >= 16 && second <= 31) {
            return true;
        }
        if (first == 192 && second == 168) {
            return true;
        }
        if (ip.equals("127.0.0.1") || ip.equals("localhost")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Testa conectividade com um host
     * 
     * @param host IP ou hostname
     * @param port Porta
     * @param timeout Timeout em milissegundos
     * @return true se conseguiu conectar
     */
    public static boolean testConnection(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            LOGGER.log(Level.INFO, "✅ Conexão bem-sucedida com {0}:{1}", 
                new Object[]{host, port});
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "❌ Falha ao conectar com {0}:{1}", 
                new Object[]{host, port});
            return false;
        }
    }
    
    /**
     * Retorna informações sobre a rede atual
     */
    public static String getNetworkInfo() {
        StringBuilder info = new StringBuilder();
        info.append("📡 Informações de Rede\n");
        info.append("═══════════════════════\n");
        
        String localIP = getLocalIP();
        info.append("🏠 IP Local (LAN): ").append(localIP).append("\n");
        
        if (isPrivateIP(localIP)) {
            info.append("   └─ Tipo: Rede Privada\n");
        }
        
        String publicIP = getPublicIP();
        if (publicIP != null) {
            info.append("🌐 IP Público (Internet): ").append(publicIP).append("\n");
            info.append("   └─ Tipo: Acessível pela Internet\n");
        } else {
            info.append("🌐 IP Público: Não disponível\n");
        }
        
        info.append("\n💡 Dica:\n");
        if (localIP.equals(publicIP)) {
            info.append("   Seu servidor está diretamente conectado à Internet!\n");
        } else {
            info.append("   Use IP Local para conexões na mesma rede (LAN)\n");
            info.append("   Use IP Público para conexões pela Internet\n");
            info.append("   (Requer Port Forwarding no roteador)\n");
        }
        
        return info.toString();
    }
    
    /**
     * Detecta automaticamente o melhor IP para usar
     * 
     * @param isLocalNetwork true se conectando na mesma rede
     * @return IP recomendado
     */
    public static String getRecommendedIP(boolean isLocalNetwork) {
        if (isLocalNetwork) {
            return getLocalIP();
        } else {
            String publicIP = getPublicIP();
            return publicIP != null ? publicIP : getLocalIP();
        }
    }
}
