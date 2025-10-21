# 🌐 Guia de Conexão pela Internet

## Problema: Conexões Entre Redes Diferentes

### ❌ Situação Atual (Apenas LAN)
```
Casa do Pedro (192.168.1.x)          Casa da Maria (192.168.0.x)
├── Servidor: 192.168.1.100          └── Cliente: 192.168.0.50
└── Cliente: 192.168.1.101                    │
         ✅ Conecta                             ❌ NÃO conecta!
                                         (IP privado inacessível)
```

**Por quê?** IPs como `192.168.x.x` são **privados** e só funcionam dentro da mesma rede local.

---

## ✅ Soluções para Conexões pela Internet

### 🔧 Opção 1: Port Forwarding (Recomendado)

**Vantagem:** Solução permanente, sem dependências externas

#### Passo 1: Descobrir IP Público

**Windows:**
```bash
# No servidor
curl ifconfig.me

# Ou visite: https://www.meuip.com.br
```

**Exemplo de resultado:** `177.45.123.89`

#### Passo 2: Configurar Roteador

1. **Acessar painel do roteador:**
   - Navegador: `http://192.168.1.1` (ou `192.168.0.1`)
   - Login: admin/admin (ou veja etiqueta do roteador)

2. **Encontrar opção "Port Forwarding" ou "Redirecionamento de Portas"**
   - Pode estar em: Avançado → NAT → Port Forwarding

3. **Adicionar regras:**

   **Regra 1 - AuthServer:**
   ```
   Nome: Pokemon-AuthServer
   Protocolo: TCP
   Porta Externa: 5555
   IP Interno: 192.168.1.100  (IP do servidor na LAN)
   Porta Interna: 5555
   ```

   **Regra 2 - BattleServer:**
   ```
   Nome: Pokemon-BattleServer
   Protocolo: TCP
   Porta Externa: 5556
   IP Interno: 192.168.1.100
   Porta Interna: 5556
   ```

4. **Salvar e Reiniciar Roteador**

#### Passo 3: Configurar Firewall do Windows

```powershell
# Executar como Administrador
# Permitir porta 5555 (AuthServer)
New-NetFirewallRule -DisplayName "Pokemon-AuthServer" -Direction Inbound -LocalPort 5555 -Protocol TCP -Action Allow

# Permitir porta 5556 (BattleServer)
New-NetFirewallRule -DisplayName "Pokemon-BattleServer" -Direction Inbound -LocalPort 5556 -Protocol TCP -Action Allow
```

#### Passo 4: Cliente Conecta

```
Servidor: 177.45.123.89  ← IP PÚBLICO (não 192.168.x.x!)
Porta: 5555
```

**Resultado:**
```
Internet
    │
    ├─→ [Roteador Pedro] IP Público: 177.45.123.89
    │        │ (Port Forward 5555 → 192.168.1.100:5555)
    │        │ (Port Forward 5556 → 192.168.1.100:5556)
    │        │
    │        └─→ [Servidor Local] 192.168.1.100
    │
    └─→ [Cliente Maria] Casa diferente
           Conecta: 177.45.123.89:5555 ✅ FUNCIONA!
```

---

### 🚀 Opção 2: Ngrok (Mais Fácil para Testes)

**Vantagem:** Não precisa mexer no roteador!  
**Desvantagem:** IP muda a cada execução (versão grátis)

#### Passo 1: Instalar Ngrok

1. Download: https://ngrok.com/download
2. Criar conta (grátis)
3. Extrair e colocar em `C:\ngrok`

#### Passo 2: Autenticar

```bash
ngrok authtoken SEU_TOKEN_AQUI
```

#### Passo 3: Iniciar Servidores

```bash
# Terminal 1 - AuthServer
cd C:\Users\pedro\Desktop\Projeto-Pokemon
java -cp target/classes demo.clientserver.AuthServer

# Terminal 2 - BattleServer
java -cp target/classes backend.network.server.BattleServer
```

#### Passo 4: Criar Túneis Ngrok

```bash
# Terminal 3 - Túnel AuthServer
ngrok tcp 5555

# Copiar URL gerada, exemplo:
# tcp://0.tcp.ngrok.io:12345

# Terminal 4 - Túnel BattleServer
ngrok tcp 5556

# Copiar URL gerada, exemplo:
# tcp://0.tcp.ngrok.io:67890
```

#### Passo 5: Cliente Conecta

**Para AuthServer:**
```
Servidor: 0.tcp.ngrok.io
Porta: 12345
```

**Para BattleServer (modificar código):**
```java
BattleClient client = new BattleClient("0.tcp.ngrok.io", 67890);
```

**Resultado:**
```
[Servidor Pedro]              [Ngrok Cloud]                [Cliente Maria]
     │                              │                            │
AuthServer:5555 ←─tunnel─→ ngrok.io:12345 ←─internet─→ Conecta! ✅
BattleServer:5556 ←─tunnel─→ ngrok.io:67890 ←─internet─→ Conecta! ✅
```

---

### 🔐 Opção 3: VPN (Simular Mesma Rede)

**Ferramentas:** Hamachi, ZeroTier, Tailscale

#### Exemplo com Hamachi:

1. **Download:** https://www.vpn.net
2. **Servidor e Cliente instalam Hamachi**
3. **Criar rede:**
   - Nome: `pokemon-battle`
   - Senha: `pikachu123`
4. **Ambos entram na mesma rede**
5. **Anotar IP da VPN** (ex: `25.45.123.50`)
6. **Cliente conecta usando IP da VPN:**
   ```
   Servidor: 25.45.123.50
   Porta: 5555
   ```

**Resultado:**
```
[Servidor Pedro]         [Hamachi VPN]         [Cliente Maria]
  IP VPN: 25.45.123.50      │                  IP VPN: 25.67.89.10
         └─────────────────VPN Tunnel──────────────────┘
                   Parecem estar na mesma rede! ✅
```

---

## 🧪 Testes de Conectividade

### Teste 1: Verificar IP Público
```bash
curl ifconfig.me
# Ou visite: https://www.meuip.com.br
```

### Teste 2: Testar Port Forward (Externo)
```bash
# De OUTRO computador (fora da rede)
telnet SEU_IP_PUBLICO 5555

# Ou use: https://www.yougetsignal.com/tools/open-ports/
```

### Teste 3: Usar NetworkUtils (Novo)
```bash
java -cp target/classes shared.util.NetworkUtils
# Exibe IP local, IP público e recomendações
```

---

## 🔍 Troubleshooting

### Problema: "Connection Refused"

**Causa 1:** Port forwarding não configurado  
**Solução:** Revisar configuração do roteador

**Causa 2:** Firewall bloqueando  
**Solução:**
```powershell
# Verificar regras de firewall
Get-NetFirewallRule | Where-Object {$_.LocalPort -eq 5555}

# Se não existir, criar:
New-NetFirewallRule -DisplayName "Pokemon-Auth" -Direction Inbound -LocalPort 5555 -Protocol TCP -Action Allow
```

### Problema: "Connection Timeout"

**Causa 1:** IP público incorreto  
**Solução:** Verificar com `curl ifconfig.me`

**Causa 2:** ISP bloqueando portas  
**Solução:** 
- Tentar portas alternativas (8080, 8888)
- Ou usar Ngrok

**Causa 3:** Router com CGNAT (IP compartilhado)  
**Solução:** Usar Ngrok ou VPN

### Problema: Funciona localmente mas não pela internet

**Causa:** Testando com IP público de dentro da mesma rede  
**Solução:** Testar de fora (usar 4G no celular ou pedir amigo)

---

## 📱 Testando com Celular (4G)

**Método rápido para testar se porta está aberta:**

1. **Servidor:** Anotar IP público (`177.45.123.89`)
2. **Celular:** Desligar Wi-Fi, usar 4G
3. **Celular:** Instalar "Network Analyzer" (Play Store)
4. **Celular:** Testar porta: `177.45.123.89:5555`
5. Se abrir = ✅ Port forwarding funciona!

---

## 🎯 Checklist Completo

**Servidor (Casa do Pedro):**
- [ ] IP público anotado
- [ ] Port forwarding configurado (5555, 5556)
- [ ] Firewall Windows configurado
- [ ] Servidores rodando (AuthServer + BattleServer)
- [ ] Testado externamente (4G ou outro PC)

**Cliente (Casa da Maria):**
- [ ] Sabe o IP público do servidor
- [ ] LoginFrame configurado com IP público
- [ ] Conexão testada e funcional

---

## 💡 Dicas Importantes

### IP Dinâmico vs Estático

**Problema:** Seu IP público pode mudar!

**Soluções:**

1. **Dynamic DNS (DDNS):**
   - Serviços: No-IP, DynDNS, Duck DNS
   - Cria um domínio: `pedropokemon.ddns.net`
   - Cliente usa domínio em vez de IP

2. **Verificar IP antes de jogar:**
   ```bash
   curl ifconfig.me
   # Avisar amigos do novo IP
   ```

### Segurança

**Atenção:** Expor portas na internet tem riscos!

**Recomendações:**
1. Usar senhas fortes
2. Não expor porta do banco de dados
3. Considerar adicionar autenticação extra
4. Usar apenas quando for jogar
5. Desabilitar port forwarding depois

---

## 📊 Comparação de Soluções

| Solução | Dificuldade | Permanente | Gratuito | Melhor Para |
|---------|-------------|------------|----------|-------------|
| Port Forwarding | Média | ✅ Sim | ✅ Sim | Uso regular |
| Ngrok | Fácil | ❌ IP muda | ✅ Sim* | Testes/Demos |
| VPN (Hamachi) | Fácil | ✅ Sim | ✅ Sim | Grupos pequenos |
| DDNS | Média | ✅ Sim | ✅ Sim | IP dinâmico |

*Ngrok grátis: 1 túnel simultâneo, IP muda

---

## 🚀 Exemplo Completo

### Cenário: Pedro (Servidor) e Maria (Cliente)

**Pedro faz:**
```bash
# 1. Descobrir IP público
curl ifconfig.me
# Resultado: 177.45.123.89

# 2. Configurar port forwarding no roteador
# 5555 → 192.168.1.100:5555
# 5556 → 192.168.1.100:5556

# 3. Iniciar servidores
java -cp target/classes demo.clientserver.AuthServer &
java -cp target/classes backend.network.server.BattleServer &

# 4. Avisar Maria: "Conecte em 177.45.123.89:5555"
```

**Maria faz:**
```bash
# 1. Iniciar jogo
java -cp target/classes app.Main

# 2. Marcar "Usar Servidor Remoto"
# 3. Servidor: 177.45.123.89
# 4. Porta: 5555
# 5. Login!

# ✅ FUNCIONA mesmo em redes diferentes!
```

---

## 📞 Suporte Rápido

**Não funcionou?** Verifique:

1. ✅ IP público correto? `curl ifconfig.me`
2. ✅ Port forwarding ativo? Teste: https://portchecker.co
3. ✅ Firewall liberado? `Get-NetFirewallRule`
4. ✅ Servidor rodando? `netstat -ano | findstr :5555`
5. ✅ Testando de FORA da rede? (use 4G)

**Ainda não funciona?** Use Ngrok (mais fácil):
```bash
ngrok tcp 5555
# Compartilha URL gerada
```

---

**🌐 Agora seu jogo é acessível de qualquer lugar do mundo! 🌐**
