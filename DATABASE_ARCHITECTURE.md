# 🗄️ Arquitetura de Banco de Dados - Multiplayer

## 🚨 Problema: Bancos de Dados Desincronizados

### ❌ **Cenário ERRADO (Modo Local)**

```
Computador do Pedro:              Computador da Maria:
┌─────────────────────┐          ┌─────────────────────┐
│ SQLite: usuarios.db │          │ SQLite: usuarios.db │
│ - pedro (senha: 123)│          │ - maria (senha: 456)│
│ - ana (senha: 789)  │          │ - joao (senha: abc) │
└─────────────────────┘          └─────────────────────┘
         ↓                                  ↓
   LoginFrame (LOCAL)               LoginFrame (LOCAL)
         ↓                                  ↓
   ✅ Pedro OK                        ❌ Pedro não existe!
```

**Problema:** Cada jogador tem SEU PRÓPRIO banco local. Maria não consegue fazer login com conta do Pedro porque o banco dela não tem essa conta.

---

## ✅ **Solução: Banco de Dados Centralizado**

### **Arquitetura Correta (Modo Remoto)**

```
                    ┌──────────────────────────────────┐
                    │  SERVIDOR (Máquina do Pedro)     │
                    │                                  │
                    │  📦 SQLite: usuarios.db          │
                    │  ┌────────────────────────────┐  │
                    │  │ Tabela: users              │  │
                    │  │ - pedro | senha_hash_123   │  │
                    │  │ - maria | senha_hash_456   │  │
                    │  │ - joao  | senha_hash_789   │  │
                    │  │ - ana   | senha_hash_abc   │  │
                    │  └────────────────────────────┘  │
                    │                                  │
                    │  🔐 AuthServer (porta 5555)      │
                    │  └─ Valida login no DB acima     │
                    │                                  │
                    │  ⚔️  BattleServer (porta 5556)   │
                    │  └─ Gerencia batalhas            │
                    │                                  │
                    │  IP: 187.20.30.40 (público)      │
                    └──────────────────────────────────┘
                             ↑              ↑
                 ┌───────────┘              └───────────┐
                 │                                      │
     ┌───────────────────────┐              ┌───────────────────────┐
     │ Pedro (Cliente)       │              │ Maria (Cliente)       │
     │                       │              │                       │
     │ LoginFrame            │              │ LoginFrame            │
     │ [x] Servidor Remoto   │              │ [x] Servidor Remoto   │
     │ Host: 187.20.30.40    │              │ Host: 187.20.30.40    │
     │ Porta: 5555           │              │ Porta: 5555           │
     │                       │              │                       │
     │ AuthClient            │              │ AuthClient            │
     │ └─ Conecta ao servidor│              │ └─ Conecta ao servidor│
     │    e valida no DB dele│              │    e valida no DB dele│
     │                       │              │                       │
     │ ✅ Login OK           │              │ ✅ Login OK           │
     └───────────────────────┘              └───────────────────────┘
```

---

## 🔧 **Configuração Passo a Passo**

### **SERVIDOR (1 pessoa hospeda)**

#### **Responsabilidades:**
- Hospedar banco de dados centralizado (`usuarios.db`)
- Rodar `AuthServer` (porta 5555)
- Rodar `BattleServer` (porta 5556)
- Configurar Port Forwarding no roteador
- Compartilhar IP público com amigos

#### **Passos:**

1. **Criar/Verificar banco de dados:**
```bash
cd Projeto-Pokemon
ls src/main/resources/usuarios.db  # Verifica se existe
```

Se não existir, o `AuthServer` cria automaticamente na primeira execução.

2. **Registrar todas as contas dos jogadores:**

**Opção A:** Usar `AdminFrame` para criar contas:
```java
// Executar Main.java
// Login como admin (se existir)
// Ou criar primeiro usuário manualmente
```

**Opção B:** Pedir que cada jogador se registre via `LoginFrame` (modo local):
```java
// Cada amigo registra sua conta localmente
// Depois você COPIA o usuarios.db deles para sua máquina
// OU todos se registram diretamente na sua máquina
```

**Opção C:** Criar contas manualmente via SQL:
```sql
-- Conectar no usuarios.db
INSERT INTO users (username, password_hash, user_type) VALUES
('pedro', 'hash_da_senha_123', 'JOGADOR'),
('maria', 'hash_da_senha_456', 'JOGADOR'),
('joao', 'hash_da_senha_789', 'JOGADOR');
```

3. **Iniciar o servidor:**
```bash
mvn clean compile
java -cp target/classes app.ServerMain
```

4. **Verificar portas abertas:**
```bash
# Windows (PowerShell)
netstat -an | Select-String "5555|5556"

# Deve mostrar:
# TCP    0.0.0.0:5555           LISTENING
# TCP    0.0.0.0:5556           LISTENING
```

5. **Configurar Port Forwarding:**
- Acesse o roteador (192.168.0.1 ou 192.168.1.1)
- Configure:
  - Porta Externa: 5555 → IP Local (192.168.x.x) : 5555
  - Porta Externa: 5556 → IP Local (192.168.x.x) : 5556
- Guia completo: `INTERNET_CONNECTION_GUIDE.md`

6. **Descobrir IP público:**
```bash
java -cp target/classes app.ServerMain
# Janela mostra automaticamente
# OU acessar: https://www.whatismyip.com/
```

7. **Compartilhar com amigos:**
```
Envie para todos os jogadores:
"Conecte em: 187.20.30.40"
```

---

### **CLIENTES (Todos os jogadores)**

#### **Responsabilidades:**
- Conectar no servidor remoto
- NÃO usar banco de dados local
- Autenticar via `AuthClient`

#### **Passos:**

1. **Compilar o jogo:**
```bash
mvn clean compile
java -cp target/classes app.Main
```

2. **Configurar LoginFrame:**
- ✅ Marcar: `[x] Usar Servidor Remoto`
- Host: `187.20.30.40` (IP do servidor)
- Porta Auth: `5555`
- Username: `pedro` (conta criada no servidor)
- Password: `123`

3. **Fazer login:**
- AuthClient conecta no `187.20.30.40:5555`
- Servidor valida no banco centralizado
- ✅ Login aprovado

4. **Jogar multiplayer:**
- Escolher equipe de 6 Pokémon
- Clicar "Batalhar Online"
- BattleClient conecta em `187.20.30.40:5556`
- Aguardar outro jogador

---

## 📊 **Comparação: Local vs Remoto**

| Aspecto | **Modo LOCAL** | **Modo REMOTO** |
|---------|---------------|----------------|
| **Banco de Dados** | Cada máquina tem o seu | Centralizado no servidor |
| **Autenticação** | UserService local (SQLite local) | AuthClient → AuthServer remoto |
| **Sincronização** | ❌ Impossível (DBs separados) | ✅ Automática (todos usam mesmo DB) |
| **Vantagens** | Simples, sem rede | Multiplayer real, contas globais |
| **Desvantagens** | Apenas 1 jogador | Precisa configurar servidor |

---

## 🔐 **Gerenciamento de Contas**

### **Como adicionar novos jogadores?**

#### **Opção 1: Registro Remoto (RECOMENDADO)**
```java
// Cliente configura LoginFrame
[x] Usar Servidor Remoto
Host: 187.20.30.40
Porta: 5555

// Clica em "Registrar"
Username: novo_jogador
Password: senha123

// AuthClient envia para AuthServer
// Servidor salva no usuarios.db centralizado
✅ Conta criada!
```

#### **Opção 2: Admin cria contas**
```java
// Servidor executa AdminFrame
// Login como admin
// Criar Usuário → novo_jogador
// Salva automaticamente no usuarios.db
```

#### **Opção 3: Registro Local + Merge**
```bash
# Jogador registra localmente
# Gera usuarios.db local

# Servidor faz merge dos bancos
sqlite3 usuarios_servidor.db
ATTACH 'usuarios_jogador.db' AS jogador_db;
INSERT INTO users SELECT * FROM jogador_db.users WHERE username NOT IN (SELECT username FROM users);
DETACH jogador_db;
```

---

## 🛡️ **Segurança**

### **Problemas Atuais:**
- ❌ Senhas trafegam em texto puro (sem HTTPS/TLS)
- ❌ Sem token de sessão (qualquer um pode enviar username)
- ❌ Sem rate limiting (possível DDoS)

### **Melhorias Futuras:**
1. **SSL/TLS:** Criptografar comunicação
2. **JWT Tokens:** Autenticação stateless
3. **Rate Limiting:** Máximo 5 tentativas/minuto
4. **IP Whitelist:** Apenas IPs conhecidos

---

## 🧪 **Testes**

### **Testar LAN (Mesma Rede):**
```bash
# Servidor
java -cp target/classes app.ServerMain
# Mostra: IP Local: 192.168.1.100

# Cliente 1 (mesma máquina)
LoginFrame → Host: localhost

# Cliente 2 (outra máquina na LAN)
LoginFrame → Host: 192.168.1.100
```

### **Testar Internet (Redes Diferentes):**
```bash
# Servidor (Pedro em casa)
java -cp target/classes app.ServerMain
# Mostra: IP Público: 187.20.30.40
# Port Forwarding: 5555, 5556 → 192.168.1.100

# Cliente (Maria em outra cidade)
LoginFrame → Host: 187.20.30.40
```

### **Testar com Ngrok (Sem Port Forwarding):**
```bash
# Servidor
java -cp target/classes app.ServerMain
ngrok tcp 5555
ngrok tcp 5556
# Mostra: tcp://0.tcp.ngrok.io:12345

# Cliente
LoginFrame → Host: 0.tcp.ngrok.io
            → Porta: 12345
```

---

## 📝 **Resumo**

### **✅ Modo Correto (Multiplayer):**
1. **1 máquina = SERVIDOR**
   - Hospeda `usuarios.db` (centralizado)
   - Roda `AuthServer` (5555) + `BattleServer` (5556)
   - Configura Port Forwarding
   - Compartilha IP público

2. **N máquinas = CLIENTES**
   - Conectam no servidor remoto
   - Autenticam via `AuthClient`
   - Batalham via `BattleClient`
   - NÃO usam banco local

### **❌ Modo Errado:**
- Cada jogador usa banco local separado
- Impossível autenticar contas de outros jogadores
- Não funciona multiplayer

---

## 🚀 **Próximos Passos**

1. ✅ Criar `ServerMain.java` (Servidor dedicado)
2. ⏳ Testar LAN (mesma rede)
3. ⏳ Configurar Port Forwarding
4. ⏳ Testar Internet (redes diferentes)
5. ⏳ Adicionar UI para escolher modo (Local vs Multiplayer)
6. ⏳ Implementar registro remoto via AuthClient

---

**Dúvidas? Consulte:**
- `INTERNET_CONNECTION_GUIDE.md` - Como configurar Port Forwarding
- `INTERNET_FAQ.md` - Perguntas sobre conexão entre redes
- `BATTLE_MULTIPLAYER_GUIDE.md` - Como jogar multiplayer
