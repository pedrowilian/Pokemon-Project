package demo.clientserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cliente de Autenticação - Demonstração Cliente-Servidor
 * 
 * Conecta ao AuthServer via Socket TCP e realiza autenticação remota
 * 
 * Protocolo:
 * 1. Envia username
 * 2. Envia password
 * 3. Recebe status ("OK" ou "ERRO")
 * 4. Recebe userType (se OK) ou mensagem de erro (se ERRO)
 */
public class AuthClient {
    private static final Logger LOGGER = Logger.getLogger(AuthClient.class.getName());
    private static final int TIMEOUT = 5000; // 5 segundos
    
    private final String serverHost;
    private final int serverPort;
    
    public AuthClient(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;
    }
    
    /**
     * Tenta fazer login no servidor remoto
     * 
     * @param username Nome de usuário
     * @param password Senha
     * @return Resultado da autenticação
     */
    public AuthResult login(String username, String password) {
        LOGGER.log(Level.INFO, "🔌 Conectando ao servidor {0}:{1}...", 
                new Object[]{serverHost, serverPort});
        
        try (
            Socket socket = new Socket();
        ) {
            // Conecta com timeout
            socket.connect(new InetSocketAddress(serverHost, serverPort), TIMEOUT);
            
            LOGGER.info("✅ Conectado ao servidor!");
            
            try (
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
            ) {
                // Envia credenciais
                LOGGER.log(Level.INFO, "📤 Enviando credenciais: {0}", username);
                out.println(username);
                out.println(password);
                
                // Recebe resposta
                String status = in.readLine();
                String message = in.readLine();
                
                if (status == null || message == null) {
                    LOGGER.warning("❌ Resposta inválida do servidor");
                    return new AuthResult(false, null, "Resposta inválida do servidor");
                }
                
                if ("OK".equals(status)) {
                    LOGGER.log(Level.INFO, "✅ Login bem-sucedido! Tipo: {0}", message);
                    return new AuthResult(true, message, null);
                } else {
                    LOGGER.log(Level.WARNING, "❌ Login falhou: {0}", message);
                    return new AuthResult(false, null, message);
                }
            }
            
        } catch (SocketTimeoutException e) {
            String error = "Tempo de conexão esgotado. Servidor não responde.";
            LOGGER.log(Level.SEVERE, "❌ {0}", error);
            return new AuthResult(false, null, error);
            
        } catch (ConnectException e) {
            String error = "Não foi possível conectar ao servidor. Verifique se o servidor está rodando.";
            LOGGER.log(Level.SEVERE, "❌ {0}", error);
            return new AuthResult(false, null, error);
            
        } catch (UnknownHostException e) {
            String error = "Host desconhecido: " + serverHost;
            LOGGER.log(Level.SEVERE, "❌ {0}", error);
            return new AuthResult(false, null, error);
            
        } catch (IOException e) {
            String error = "Erro de conexão: " + e.getMessage();
            LOGGER.log(Level.SEVERE, "❌ Erro de I/O", e);
            return new AuthResult(false, null, error);
        }
    }
    
    /**
     * Testa conexão com o servidor
     * 
     * @return true se servidor está acessível
     */
    public boolean testConnection() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(serverHost, serverPort), TIMEOUT);
            LOGGER.log(Level.INFO, "✅ Servidor acessível em {0}:{1}", 
                    new Object[]{serverHost, serverPort});
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "❌ Servidor inacessível em {0}:{1}", 
                    new Object[]{serverHost, serverPort});
            return false;
        }
    }
    
    /**
     * Resultado de autenticação
     */
    public static class AuthResult {
        public final boolean success;
        public final String userType;  // "ADMIN" ou "JOGADOR" (se success = true)
        public final String error;      // Mensagem de erro (se success = false)
        
        public AuthResult(boolean success, String userType, String error) {
            this.success = success;
            this.userType = userType;
            this.error = error;
        }
        
        @Override
        public String toString() {
            if (success) {
                return "AuthResult{success=true, userType='" + userType + "'}";
            } else {
                return "AuthResult{success=false, error='" + error + "'}";
            }
        }
    }
    
    /**
     * Main - Teste standalone
     */
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║      POKEMON PROJECT - CLIENTE DE AUTENTICAÇÃO          ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // Configuração
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5555;
        
        AuthClient client = new AuthClient(host, port);
        
        // Testa conexão
        System.out.println("🔍 Testando conexão com servidor...");
        if (!client.testConnection()) {
            System.out.println("❌ Falha! Servidor não está rodando.");
            System.exit(1);
        }
        
        System.out.println("✅ Servidor está rodando!");
        System.out.println();
        
        // Testa login
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.print("Username: ");
            String username = reader.readLine();
            
            System.out.print("Password: ");
            String password = reader.readLine();
            
            System.out.println("\n🔐 Autenticando...");
            AuthResult result = client.login(username, password);
            
            System.out.println("\n" + "=".repeat(60));
            if (result.success) {
                System.out.println("✅ LOGIN BEM-SUCEDIDO!");
                System.out.println("   Usuário: " + username);
                System.out.println("   Tipo: " + result.userType);
            } else {
                System.out.println("❌ LOGIN FALHOU!");
                System.out.println("   Erro: " + result.error);
            }
            System.out.println("=".repeat(60));
            
        } catch (IOException e) {
            System.err.println("Erro ao ler entrada: " + e.getMessage());
        }
    }
}
