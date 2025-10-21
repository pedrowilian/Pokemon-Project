package app;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import backend.network.server.BattleServer;
import demo.clientserver.AuthServer;
import shared.util.NetworkUtils;

/**
 * Servidor dedicado - Roda AuthServer + BattleServer
 * 
 * USO:
 * - 1 pessoa hospeda este servidor em sua máquina
 * - Configura Port Forwarding no roteador (portas 5555 e 5556)
 * - Compartilha IP público com amigos
 * - Todos os clientes se conectam neste servidor
 * 
 * BANCO DE DADOS:
 * - usuarios.db fica nesta máquina (centralizado)
 * - Todos os jogadores autenticam aqui
 * 
 * COMO EXECUTAR:
 * mvn clean compile
 * java -cp target/classes app.ServerMain
 */
public class ServerMain {
    private static final Logger LOGGER = Logger.getLogger(ServerMain.class.getName());
    
    public static void main(String[] args) {
        // Mostra janela com informações do servidor
        SwingUtilities.invokeLater(() -> createServerUI());
        
        // Inicia AuthServer (porta 5555)
        new Thread(() -> {
            try {
                AuthServer authServer = new AuthServer();
                authServer.start();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "❌ Erro ao iniciar AuthServer", e);
            }
        }, "AuthServer-Thread").start();
        
        // Inicia BattleServer (porta 5556)
        new Thread(() -> {
            try {
                BattleServer battleServer = new BattleServer();
                battleServer.start();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "❌ Erro ao iniciar BattleServer", e);
            }
        }, "BattleServer-Thread").start();
        
        LOGGER.log(Level.INFO, "🌐 Servidores iniciados!");
    }
    
    private static void createServerUI() {
        JFrame frame = new JFrame("🌐 Servidor Pokemon - Multiplayer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setLocationRelativeTo(null);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(45, 52, 90));
        
        // Título
        JLabel titleLabel = new JLabel("🌐 Servidor Pokemon");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(20));
        
        // Status
        JLabel statusLabel = new JLabel("✅ Servidores Ativos");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 18));
        statusLabel.setForeground(new Color(76, 175, 80));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(statusLabel);
        panel.add(Box.createVerticalStrut(30));
        
        // Informações de conexão
        JTextArea infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        infoArea.setBackground(new Color(30, 35, 60));
        infoArea.setForeground(Color.WHITE);
        infoArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 110, 150), 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        StringBuilder info = new StringBuilder();
        info.append("╔════════════════════════════════════════════╗\n");
        info.append("║         INFORMAÇÕES DE CONEXÃO            ║\n");
        info.append("╠════════════════════════════════════════════╣\n");
        info.append("║                                            ║\n");
        info.append("║  📡 AuthServer                            ║\n");
        info.append("║     Porta: 5555                           ║\n");
        info.append("║                                            ║\n");
        info.append("║  ⚔️  BattleServer                         ║\n");
        info.append("║     Porta: 5556                           ║\n");
        info.append("║                                            ║\n");
        info.append("╠════════════════════════════════════════════╣\n");
        info.append("║  🏠 Rede Local (LAN)                      ║\n");
        info.append("║     IP: ").append(String.format("%-31s", NetworkUtils.getLocalIP())).append("║\n");
        info.append("║                                            ║\n");
        info.append("║  🌍 Internet (WAN)                        ║\n");
        
        String publicIP = NetworkUtils.getPublicIP();
        if (publicIP != null && !publicIP.equals("Indisponível")) {
            info.append("║     IP: ").append(String.format("%-31s", publicIP)).append("║\n");
        } else {
            info.append("║     IP: Indisponível (sem internet)      ║\n");
        }
        
        info.append("║                                            ║\n");
        info.append("╠════════════════════════════════════════════╣\n");
        info.append("║  📋 INSTRUÇÕES PARA JOGADORES:            ║\n");
        info.append("║                                            ║\n");
        info.append("║  1. Abrir LoginFrame                      ║\n");
        info.append("║  2. Marcar [x] Usar Servidor Remoto      ║\n");
        info.append("║  3. Host: <IP ACIMA>                      ║\n");
        info.append("║  4. Porta: 5555                           ║\n");
        info.append("║  5. Fazer login normalmente               ║\n");
        info.append("║                                            ║\n");
        info.append("╠════════════════════════════════════════════╣\n");
        info.append("║  ⚠️  ATENÇÃO:                              ║\n");
        info.append("║                                            ║\n");
        info.append("║  - Para LAN: Use o IP Local               ║\n");
        info.append("║  - Para Internet: Configure Port Forward  ║\n");
        info.append("║    (Ver INTERNET_CONNECTION_GUIDE.md)     ║\n");
        info.append("║                                            ║\n");
        info.append("╚════════════════════════════════════════════╝\n");
        
        infoArea.setText(info.toString());
        infoArea.setCaretPosition(0);
        
        JScrollPane scrollPane = new JScrollPane(infoArea);
        scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(scrollPane);
        
        panel.add(Box.createVerticalStrut(20));
        
        // Botão copiar IP
        JButton copyButton = new JButton("📋 Copiar IP Público");
        copyButton.setFont(new Font("Arial", Font.BOLD, 14));
        copyButton.setBackground(new Color(33, 150, 243));
        copyButton.setForeground(Color.WHITE);
        copyButton.setFocusPainted(false);
        copyButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        copyButton.addActionListener(e -> {
            String ip = publicIP != null ? publicIP : NetworkUtils.getLocalIP();
            java.awt.datatransfer.StringSelection selection = 
                new java.awt.datatransfer.StringSelection(ip);
            java.awt.Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(selection, selection);
            JOptionPane.showMessageDialog(frame, 
                "✅ IP copiado: " + ip, 
                "Sucesso", 
                JOptionPane.INFORMATION_MESSAGE);
        });
        panel.add(copyButton);
        
        frame.add(panel);
        frame.setVisible(true);
        
        LOGGER.log(Level.INFO, "🖥️  Interface do servidor iniciada");
    }
}
