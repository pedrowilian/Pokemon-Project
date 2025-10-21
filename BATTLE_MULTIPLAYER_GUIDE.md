# 🎮 Guia de Batalhas Multiplayer

## 📡 Sistema Cliente-Servidor Completo

O projeto agora possui **2 servidores** independentes:

### 1️⃣ AuthServer (Porta 5555)
- **Função:** Autenticação de usuários
- **Uso:** Login remoto
- **Iniciar:** `java -cp target/classes demo.clientserver.AuthServer`

### 2️⃣ BattleServer (Porta 5556)
- **Função:** Gerenciar batalhas multiplayer
- **Uso:** Batalhas entre 2 jogadores remotos
- **Iniciar:** `java -cp target/classes backend.network.server.BattleServer`

---

## 🎯 Modos de Jogo

### Modo 1: Local (Contra IA)
```
┌────────────────┐
│   CLIENTE      │
│                │
│  Battle vs IA  │  ← Sem rede
│                │
└────────────────┘
```

**Como jogar:**
1. Não marcar "Usar Servidor Remoto"
2. Login local
3. Batalha contra IA

---

### Modo 2: Multiplayer Remoto
```
┌────────────┐          ┌──────────────┐          ┌────────────┐
│ CLIENTE 1  │  ←───→   │ BattleServer │  ←───→   │ CLIENTE 2  │
│ (Jogador)  │  Socket  │  Port 5556   │  Socket  │ (Jogador)  │
└────────────┘          └──────────────┘          └────────────┘
```

**Como jogar:**
1. **Máquina Servidor:**
   ```bash
   # Iniciar AuthServer (autenticação)
   java -cp target/classes demo.clientserver.AuthServer
   
   # Iniciar BattleServer (batalhas)
   java -cp target/classes backend.network.server.BattleServer
   ```

2. **Máquina Cliente 1:**
   ```bash
   java -cp target/classes app.Main
   ```
   - Marcar ✅ "Usar Servidor Remoto"
   - Servidor: `192.168.1.XXX` (IP do servidor)
   - Fazer login
   - Escolher time
   - **Selecionar "Multiplayer"** na tela de batalha
   - Aguardar oponente...

3. **Máquina Cliente 2:**
   ```bash
   java -cp target/classes app.Main
   ```
   - Marcar ✅ "Usar Servidor Remoto"
   - Servidor: `192.168.1.XXX` (mesmo IP)
   - Fazer login (usuário diferente!)
   - Escolher time
   - **Selecionar "Multiplayer"**
   - ⚔️ **Batalha começa!**

---

## 🔧 Descobrir IP do Servidor

**Windows:**
```bash
ipconfig
# Procurar "IPv4" (ex: 192.168.1.100)
```

**Linux/Mac:**
```bash
ifconfig
# ou
ip addr show
```

---

## 🛡️ Configurar Firewall

### Windows
1. Painel de Controle → Firewall
2. Configurações Avançadas
3. Regras de Entrada → Nova Regra
4. **Porta TCP 5555** (AuthServer)
5. **Porta TCP 5556** (BattleServer)
6. Permitir conexão

### Linux
```bash
sudo ufw allow 5555/tcp
sudo ufw allow 5556/tcp
```

---

## 🎮 Fluxo de Batalha Multiplayer

### Cliente 1
1. Conecta ao BattleServer
2. Chama `joinQueue(username)`
3. Aguarda... ⏳
4. Servidor encontra oponente
5. Batalha começa! ⚔️
6. Turno 1: Cliente 1 joga
7. Aguarda Cliente 2...
8. Recebe atualização do servidor
9. Próximo turno...

### Cliente 2
1. Conecta ao BattleServer
2. Chama `joinQueue(username)`
3. Servidor: "Oponente encontrado!" ✅
4. Batalha começa! ⚔️
5. Aguarda Cliente 1...
6. Turno 2: Cliente 2 joga
7. Recebe atualização do servidor
8. Próximo turno...

---

## 📡 Protocolo de Comunicação

### Entrar na Fila
**Request:**
```json
{
  "service": "BattleService",
  "method": "JOIN_QUEUE",
  "params": {
    "username": "pedro"
  }
}
```

**Response (Aguardando):**
```json
{
  "success": true,
  "data": {
    "status": "WAITING_OPPONENT",
    "battleId": "battle-abc123"
  }
}
```

**Response (Oponente Encontrado):**
```json
{
  "success": true,
  "data": {
    "status": "BATTLE_STARTED",
    "battleId": "battle-abc123",
    "opponent": "maria"
  }
}
```

### Atacar
**Request:**
```json
{
  "service": "BattleService",
  "method": "ATTACK",
  "params": {
    "moveIndex": 0
  }
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "status": "ATTACK_SUCCESS",
    "nextTurn": "maria"
  }
}
```

**Broadcast (Enviado para ambos):**
```json
{
  "data": {
    "event": "ATTACK",
    "attacker": "pedro",
    "moveIndex": 0,
    "nextTurn": "maria"
  }
}
```

---

## 🔍 Testar Conectividade

### Teste 1: Ping
```bash
ping 192.168.1.100
```

### Teste 2: Telnet (Porta)
```bash
telnet 192.168.1.100 5555
telnet 192.168.1.100 5556
```

### Teste 3: AuthClient Standalone
```bash
java -cp target/classes demo.clientserver.AuthClient localhost 5555
```

### Teste 4: BattleClient Standalone
```bash
java -cp target/classes backend.network.client.BattleClient localhost
```

---

## ⚠️ Troubleshooting

### "Connection refused"
**Causa:** Servidor não está rodando  
**Solução:**
```bash
# Verificar se servidores estão rodando
netstat -ano | findstr :5555
netstat -ano | findstr :5556

# Iniciar servidores
java -cp target/classes demo.clientserver.AuthServer &
java -cp target/classes backend.network.server.BattleServer &
```

### "Aguardando oponente..." infinitamente
**Causa:** Apenas 1 jogador na fila  
**Solução:** Conectar 2º jogador (mesma rede, mesmo servidor)

### "Não é seu turno"
**Causa:** Tentou jogar fora do turno  
**Solução:** Aguardar o oponente jogar

### Firewall bloqueia
**Windows:** Adicionar exceção para Java  
**Linux:** `sudo ufw allow 5555:5556/tcp`

---

## 📊 Arquitetura Final

```
┌─────────────────────────────────────────────────────────────────┐
│                     MÁQUINA SERVIDOR                            │
│                                                                 │
│  ┌──────────────────┐           ┌──────────────────┐           │
│  │   AuthServer     │           │  BattleServer    │           │
│  │   Port: 5555     │           │   Port: 5556     │           │
│  │   (Login)        │           │   (Batalhas)     │           │
│  └──────────────────┘           └──────────────────┘           │
│          ▲                               ▲                     │
└──────────┼───────────────────────────────┼─────────────────────┘
           │                               │
           │ Socket TCP                    │ Socket TCP
           │                               │
     ┌─────┴─────┐                   ┌─────┴─────┐
     │           │                   │           │
┌────▼─────┐ ┌──▼───────┐      ┌────▼─────┐ ┌──▼───────┐
│ Cliente1 │ │ Cliente2 │      │ Cliente1 │ │ Cliente2 │
│ Login    │ │ Login    │      │ Battle   │ │ Battle   │
└──────────┘ └──────────┘      └──────────┘ └──────────┘
  Jogador 1    Jogador 2         Jogador 1    Jogador 2
```

---

## ✅ Checklist Completo

**Servidor:**
- [ ] Java 17+ instalado
- [ ] Projeto compilado (`mvn compile`)
- [ ] Firewall configurado (portas 5555, 5556)
- [ ] AuthServer rodando
- [ ] BattleServer rodando
- [ ] IP anotado (`ipconfig`)

**Cliente:**
- [ ] Java 17+ instalado
- [ ] Projeto compilado
- [ ] IP do servidor conhecido
- [ ] Conectividade testada (`ping`)
- [ ] Usuário único (diferente do oponente)

---

## 🎓 Para Apresentação

1. **Mostrar Arquitetura** (2 min)
   - 2 servidores independentes
   - Clientes múltiplos

2. **Demo Login Remoto** (3 min)
   - Conectar Cliente 1
   - Conectar Cliente 2
   - Ambos autenticados

3. **Demo Batalha Local** (5 min)
   - Modo local vs IA
   - Funcionalidade baseline

4. **🌟 Demo Batalha Multiplayer** (10 min)
   - Servidor rodando
   - Cliente 1: Entrar na fila
   - Cliente 2: Entrar na fila
   - **Batalha começa!**
   - Turnos alternados
   - Sincronização em tempo real

5. **Código** (5 min)
   - `BattleServer.java` - Matchmaking
   - `BattleClient.java` - Socket client
   - Protocolo JSON

---

**Status:** ⚠️ **Em Desenvolvimento**  
**Próximo:** Integrar BattleClient no EnhancedBattlePanel

**🎮 Prepare-se para a batalha! 🎮**
