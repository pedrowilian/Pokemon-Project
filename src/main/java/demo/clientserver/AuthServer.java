package demo.clientserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import backend.application.service.UserService;
import backend.infrastructure.ServiceLocator;
import backend.infrastructure.security.RateLimiter;

/**
 * Servidor de Autenticação - Demonstração Cliente-Servidor
 * 
 * Funcionalidades:
 * - Java Socket (ServerSocket TCP)
 * - Thread Pool (ExecutorService)
 * - Comunicação Bidirecional
 * - Rede Local
 * 
 * Protocolo Simples:
 * Cliente envia:
 *   1. username (linha 1)
 *   2. password (linha 2)
 * 
 * Servidor responde:
 *   1. status: "OK" ou "ERRO" (linha 1)
 *   2. se OK: userType ("ADMIN" ou "JOGADOR") (linha 2)
 *      se ERRO: mensagem de erro (linha 2)
 */
public class AuthServer {
    private static final Logger LOGGER = Logger.getLogger(AuthServer.class.getName());
    private static final int PORT = 5555;
    private static final int MAX_THREADS = 10;
    
    private final ExecutorService threadPool;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    
    public AuthServer() {
        this.threadPool = Executors.newFixedThreadPool(MAX_THREADS);
    }
    
    /**
     * Inicia o servidor
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(PORT);
        running = true;
        
        LOGGER.info("=".repeat(60));
        LOGGER.log(Level.INFO, "🚀 Servidor de Autenticação Pokemon iniciado!");
        LOGGER.log(Level.INFO, "📡 Porta: {0}", PORT);
        LOGGER.log(Level.INFO, "🧵 Thread Pool: {0} threads", MAX_THREADS);
        LOGGER.info("⏳ Aguardando conexões de clientes...");
        LOGGER.info("=".repeat(60));
        
        // Loop principal - aceita conexões
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                String clientIP = clientSocket.getInetAddress().getHostAddress();
                
                // Rate limiting - proteção contra DDoS
                if (!RateLimiter.allowRequest(clientIP)) {
                    LOGGER.log(Level.WARNING, "🚫 Conexão bloqueada (rate limit): {0}", clientIP);
                    clientSocket.close();
                    continue;
                }
                
                LOGGER.log(Level.INFO, "✅ Nova conexão recebida de: {0}", clientIP);
                
                // Delega para thread pool
                threadPool.execute(() -> handleClient(clientSocket));
                
            } catch (IOException e) {
                if (running) {
                    LOGGER.log(Level.SEVERE, "❌ Erro ao aceitar conexão", e);
                }
            }
        }
    }
    
    /**
     * Processa requisição de um cliente (roda em thread separada)
     */
    private void handleClient(Socket socket) {
        String clientIP = socket.getInetAddress().getHostAddress();
        Thread currentThread = Thread.currentThread();
        
        LOGGER.log(Level.INFO, "[Thread-{0}] Processando cliente: {1}", 
                new Object[]{currentThread.getId(), clientIP});
        
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            // Recebe username
            String username = in.readLine();
            if (username == null) {
                LOGGER.log(Level.WARNING, "[Thread-{0}] Cliente {1} desconectou antes de enviar username", 
                        new Object[]{currentThread.getId(), clientIP});
                return;
            }
            
            // Recebe password
            String password = in.readLine();
            if (password == null) {
                LOGGER.log(Level.WARNING, "[Thread-{0}] Cliente {1} desconectou antes de enviar password", 
                        new Object[]{currentThread.getId(), clientIP});
                return;
            }
            
            LOGGER.log(Level.INFO, "[Thread-{0}] 🔐 Tentativa de login: {1} (de {2})", 
                    new Object[]{currentThread.getId(), username, clientIP});
            
            // Autentica usando backend service
            UserService userService = ServiceLocator.getInstance().getUserService();
            
            try {
                boolean authenticated = userService.authenticate(username, password);
                
                if (authenticated) {
                    boolean isAdmin = userService.isAdmin(username);
                    String userType = isAdmin ? "ADMIN" : "JOGADOR";
                    
                    // Envia resposta de sucesso
                    out.println("OK");
                    out.println(userType);
                    
                    LOGGER.log(Level.INFO, "[Thread-{0}] ✅ Login BEM-SUCEDIDO: {1} ({2}) de {3}", 
                            new Object[]{currentThread.getId(), username, userType, clientIP});
                    
                } else {
                    // Envia resposta de erro
                    out.println("ERRO");
                    out.println("Credenciais inválidas");
                    
                    LOGGER.log(Level.WARNING, "[Thread-{0}] ❌ Login FALHOU: {1} de {2} (credenciais inválidas)", 
                            new Object[]{currentThread.getId(), username, clientIP});
                }
                
            } catch (Exception e) {
                out.println("ERRO");
                out.println("Erro no servidor: " + e.getMessage());
                
                LOGGER.log(Level.SEVERE, "[Thread-" + currentThread.getId() + "] ❌ Erro ao autenticar: " + username, e);
            }
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "[Thread-{0}] ❌ Erro de I/O com cliente {1}: {2}", 
                    new Object[]{currentThread.getId(), clientIP, e.getMessage()});
        } finally {
            try {
                socket.close();
                LOGGER.log(Level.INFO, "[Thread-{0}] 🔌 Conexão fechada com {1}", 
                        new Object[]{currentThread.getId(), clientIP});
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "[Thread-{0}] Erro ao fechar socket", currentThread.getId());
            }
        }
    }
    
    /**
     * Para o servidor gracefully
     */
    public void stop() {
        running = false;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Erro ao fechar servidor", e);
        }
        
        threadPool.shutdown();
        LOGGER.info("🛑 Servidor encerrado");
    }
    
    /**
     * Main - Inicializa servidor
     */
    public static void main(String[] args) {
        // Banner
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║         POKEMON PROJECT - SERVIDOR DE AUTENTICAÇÃO        ║");
        System.out.println("║                                                           ║");
        System.out.println("║  Tecnologias:                                             ║");
        System.out.println("║    ✓ Java Sockets (TCP)                                  ║");
        System.out.println("║    ✓ Thread Pool (10 threads simultâneas)                ║");
        System.out.println("║    ✓ Comunicação Bidirecional                            ║");
        System.out.println("║    ✓ Rede Local (mesma internet)                         ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
        System.out.println();
        
        AuthServer server = new AuthServer();
        
        // Shutdown hook para encerramento graceful
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Encerrando servidor...");
            server.stop();
        }));
        
        try {
            server.start();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "❌ Falha ao iniciar servidor", e);
            System.err.println("\n❌ ERRO: Não foi possível iniciar o servidor na porta " + PORT);
            System.err.println("   Verifique se a porta já está em uso.");
            System.exit(1);
        }
    }
}
