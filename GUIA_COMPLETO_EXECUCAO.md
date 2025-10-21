# 🎮 Guia Completo de Execução - Projeto Pokémon

## 📋 Índice

1. [Pré-requisitos](#-pré-requisitos)
2. [Modo 1: Jogo Local (Contra IA)](#-modo-1-jogo-local-contra-ia)
3. [Modo 2: Servidor Local/LAN (Mesma Rede)](#-modo-2-servidor-locallan-mesma-rede)
4. [Modo 3: Servidor Internet (Redes Diferentes)](#-modo-3-servidor-internet-redes-diferentes)
5. [Troubleshooting](#-troubleshooting)
6. [FAQ](#-faq)

---

## 🔧 Pré-requisitos

### Software Necessário

✅ **Java 17 ou superior**
```bash
# Verificar versão do Java
java -version

# Resultado esperado:
# java version "17.0.x" ou superior
```

✅ **Maven 3.6 ou superior**
```bash
# Verificar versão do Maven
mvn -version

# Resultado esperado:
# Apache Maven 3.6.x ou superior
```

### Compilar o Projeto

**Antes de executar qualquer modo, compile o projeto:**

```bash
# Navegue até a pasta do projeto
cd C:\Users\pedro\Desktop\Projeto-Pokemon

# Compile o projeto
mvn clean compile

# Resultado esperado:
# [INFO] BUILD SUCCESS
# [INFO] Total time: X.XXX s
```

✅ **Compilação bem-sucedida:** 63 arquivos compilados sem erros

---

## 🎯 Modo 1: Jogo Local (Contra IA)

**Descrição:** Jogue contra a inteligência artificial sem necessidade de internet ou rede local.

### Passo a Passo

#### 1. Iniciar o Jogo

**Opção A: Via Maven (Recomendado)**
```bash
cd C:\Users\pedro\Desktop\Projeto-Pokemon
mvn exec:java -Dexec.mainClass="app.Main"
```

**Opção B: Via Java diretamente**
```bash
cd C:\Users\pedro\Desktop\Projeto-Pokemon\target\classes
java app.Main
```

**Opção C: Via IDE (VS Code)**
- Abrir o arquivo `src/main/java/app/Main.java`
- Clicar no botão ▶️ "Run" que aparece acima do método `main()`

#### 2. Fazer Login

![Tela de Login](docs/images/login-screen.png)

**Opção A: Login Local (Padrão)**
- Usuário: `admin`
- Senha: `admin123`
- Deixar **desmarcada** a opção "🌐 Autenticar no Servidor Remoto"
- Clicar em "Entrar"

**Opção B: Criar Nova Conta Local**
- Clicar em "Registrar"
- Preencher usuário e senha
- Deixar **desmarcada** a opção "🌐 Autenticar no Servidor Remoto"
- Clicar em "Registrar"

#### 3. Navegar pela Pokédex

- Use os **filtros** (Tipo, HP, Attack, Defense, etc.)
- Clique em um Pokémon para ver detalhes
- Use o campo "ID" para buscar diretamente

#### 4. Selecionar Time

- Clique em **"BATALHAR"** no topo da tela
- Selecione **5 Pokémons** da lista
- Clique em um Pokémon já selecionado para remover

#### 5. Iniciar Batalha Local

- Clique em **"INICIAR BATALHA"**
- No diálogo que aparece, escolha **"🏠 Modo Local"**
- A batalha contra IA começará automaticamente!

#### 6. Batalhar

**Controles:**
- **Ataque:** Clique em um dos 4 botões de movimento
- **Trocar Pokémon:** Clique em "TROCAR POKÉMON" e selecione outro
- **Fugir:** Clique em "FUGIR" para encerrar a batalha

**Regras:**
- Turnos alternados: você ataca, depois o adversário
- Pokémon desmaia quando HP chega a zero
- Vitória: derrotar todos os Pokémons adversários
- Derrota: todos os seus Pokémons desmaiarem

#### 7. Encerrar

- Após a batalha, você retorna automaticamente à Pokédex
- Use "Sair" para fechar o jogo

---

## 🌐 Modo 2: Servidor Local/LAN (Mesma Rede)

**Descrição:** Jogue contra outro jogador na **mesma rede local** (Wi-Fi ou cabo Ethernet).

**Cenário típico:**
- Dois computadores na mesma casa/escritório
- Conectados ao mesmo roteador Wi-Fi
- Ou conectados via cabo na mesma LAN

### Arquitetura

```
┌─────────────────┐         ┌─────────────────┐
│   Computador 1  │         │   Computador 2  │
│   (SERVIDOR)    │◄───────►│    (CLIENTE)    │
│                 │   LAN   │                 │
│  IP: 192.168... │         │  IP: 192.168... │
│  Portas:        │         │                 │
│  - 5555 (Auth)  │         │  Conecta em:    │
│  - 5556 (Battle)│         │  192.168.x.x    │
└─────────────────┘         └─────────────────┘
```

---

### PARTE A: Configurar o Servidor (Computador 1)

#### 1. Descobrir o IP Local do Servidor

**Windows:**
```powershell
ipconfig

# Procure por "Endereço IPv4" na seção do adaptador ativo
# Exemplo: 192.168.1.100
```

**Linux/Mac:**
```bash
ifconfig

# ou
ip addr show

# Procure pelo IP que começa com 192.168.x.x ou 10.0.x.x
```

**Exemplo de saída:**
```
Adaptador de Rede sem Fio Wi-Fi:
   Endereço IPv4. . . . . . . . . : 192.168.1.100
   Máscara de Sub-rede . . . . . . : 255.255.255.0
   Gateway Padrão. . . . . . . . . : 192.168.1.1
```

✅ **Anote esse IP:** `192.168.1.100` (exemplo)

#### 2. Iniciar o Servidor de Autenticação

**Terminal 1:**
```bash
cd C:\Users\pedro\Desktop\Projeto-Pokemon
mvn exec:java -Dexec.mainClass="backend.network.server.AuthServer"
```

**Saída esperada:**
```
🔐 Servidor de Autenticação iniciado na porta 5555
📡 IP Local: 192.168.1.100
🌍 IP Público: XX.XX.XX.XX (não usado em LAN)
✅ Aguardando conexões...
```

#### 3. Iniciar o Servidor de Batalhas

**Terminal 2 (novo terminal):**
```bash
cd C:\Users\pedro\Desktop\Projeto-Pokemon
mvn exec:java -Dexec.mainClass="backend.network.server.BattleServer"
```

**Saída esperada:**
```
⚔️ Servidor de Batalhas iniciado na porta 5556
📡 IP Local: 192.168.1.100
🌍 IP Público: XX.XX.XX.XX (não usado em LAN)
✅ Aguardando conexões...
```

#### 4. (Opcional) Usar Interface Gráfica do Servidor

**Terminal 1 (alternativa):**
```bash
cd C:\Users\pedro\Desktop\Projeto-Pokemon
mvn exec:java -Dexec.mainClass="backend.network.server.ServerMain"
```

**Interface gráfica mostrará:**
- ✅ Status dos servidores (Auth + Battle)
- 📊 Número de clientes conectados
- 📋 Logs de atividades
- 🔴 Botão para parar servidores

#### 5. Verificar Portas Abertas (Opcional)

```bash
# Windows
netstat -an | findstr "5555 5556"

# Linux/Mac
netstat -an | grep "5555\|5556"

# Resultado esperado:
# TCP    0.0.0.0:5555    0.0.0.0:0    LISTENING
# TCP    0.0.0.0:5556    0.0.0.0:0    LISTENING
```

✅ **Servidor configurado!** Prossiga para configurar os clientes.

---

### PARTE B: Configurar o Cliente (Computador 2)

#### 1. Anotar o IP do Servidor

**Informação necessária do Computador 1 (Servidor):**
- **IP Local:** `192.168.1.100` (exemplo obtido no passo A1)
- **Porta Autenticação:** `5555`
- **Porta Batalhas:** `5556`

#### 2. Iniciar o Jogo

```bash
cd C:\Users\pedro\Desktop\Projeto-Pokemon
mvn exec:java -Dexec.mainClass="app.Main"
```

#### 3. Login Remoto

![Login Remoto](docs/images/remote-login.png)

1. Na tela de login:
   - **Marcar** ✅ "🌐 Autenticar no Servidor Remoto"
   - **Host do Servidor:** `192.168.1.100` (IP do servidor)
   - **Porta:** `5555`
   - **Usuário:** Escolha um nome (ex: "jogador1")
   - **Senha:** Escolha uma senha

2. Clicar em **"Registrar"** (primeira vez) ou **"Entrar"** (se já registrou)

**Resultado esperado:**
```
✅ Conectado ao servidor remoto!
✅ Autenticação bem-sucedida!
```

**Se houver erro:**
```
❌ Erro de conexão: Connection refused
```
→ Verifique se o servidor está rodando e se o IP está correto

#### 4. Navegar pela Pokédex e Selecionar Time

- Mesmos passos do Modo Local
- Selecione 5 Pokémons
- Clique em **"INICIAR BATALHA"**

#### 5. Iniciar Batalha Multiplayer

1. No diálogo de modo de batalha, escolha **"🌐 Modo Multiplayer"**

2. Na tela de configuração:
   - **Host:** `192.168.1.100` (IP do servidor)
   - **Porta:** `5556` (porta de batalhas)
   - Clique em **"OK"**

**Resultado esperado:**
```
⏳ Aguardando oponente...
Sala: BATTLE-1234
```

#### 6. Aguardar Segundo Jogador

- O jogo ficará esperando até que outro jogador entre na fila
- Quando o oponente conectar:

```
⚔️ Oponente encontrado!
Adversário: jogador2
Sala: BATTLE-1234

A batalha começará em instantes!
```

---

### PARTE C: Configurar Segundo Cliente (Computador 3 - Opcional)

**Se você quer testar com 3+ máquinas:**

1. Repita os passos da **PARTE B** em outro computador
2. Use o **mesmo IP do servidor** (`192.168.1.100`)
3. Use um **nome de usuário diferente** (ex: "jogador2")
4. Ambos os jogadores entram na fila e são automaticamente pareados

---

### PARTE D: Batalhar em Modo Multiplayer

#### Mecânica de Turnos

**Jogador 1 (quem conectou primeiro):**
- Escolhe ataque primeiro
- Aguarda oponente escolher ataque
- Sistema calcula dano simultaneamente
- Repete até fim da batalha

**Jogador 2 (quem conectou depois):**
- Aguarda Jogador 1 escolher ataque
- Escolhe seu ataque
- Sistema calcula dano simultaneamente
- Repete até fim da batalha

#### Controles

**Durante seu turno:**
- Clique em um dos **4 movimentos** para atacar
- Ou clique em **"TROCAR POKÉMON"** para trocar
- Ou clique em **"🏳️ DESISTIR"** para desistir

**Durante turno do oponente:**
- Todos os botões ficam **desabilitados**
- Mensagem: "Aguardando oponente..."
- **Timeout de 30 segundos** - se o oponente demorar, você vence automaticamente

#### Desistência

1. Clique em **"🏳️ DESISTIR"**
2. Confirme no diálogo:
   ```
   ⚠️ Tem certeza que deseja desistir?
   Você perderá a batalha!
   ```
3. Se confirmar:
   ```
   🏳️ Você desistiu da batalha!
   ```
4. O oponente recebe vitória automática

#### Timeout

Se o oponente demorar mais de **30 segundos** para jogar:
```
⏰ Oponente demorou muito!
Você vence por timeout!
```

#### Fim da Batalha

**Vitória:**
```
VITÓRIA! Você derrotou o Rival!

PARABÉNS!
Você derrotou seu rival!
Você é o Campeão!
```

**Derrota:**
```
DERROTA! Todos os seus Pokémons desmaiaram!

Oh não!
Todos os seus Pokémons desmaiaram!
Não desista, Treinador!
```

---

### Checklist de Teste LAN

- [ ] Servidor Auth rodando na porta 5555
- [ ] Servidor Battle rodando na porta 5556
- [ ] IP do servidor anotado (`192.168.x.x`)
- [ ] Cliente 1 consegue fazer login remoto
- [ ] Cliente 1 entra na fila de batalha
- [ ] Cliente 2 consegue fazer login remoto
- [ ] Cliente 2 entra na fila de batalha
- [ ] Sistema pareia os dois jogadores automaticamente
- [ ] Turnos alternados funcionam corretamente
- [ ] Desistência funciona corretamente
- [ ] Timeout (30s) funciona corretamente
- [ ] Fim de batalha retorna à Pokédex

---

## 🌍 Modo 3: Servidor Internet (Redes Diferentes)

**Descrição:** Jogue contra outro jogador em **redes diferentes** (casas diferentes, cidades diferentes, países diferentes).

**Cenário típico:**
- Você em casa, amigo em outra casa
- Vocês não estão na mesma rede Wi-Fi
- Precisam usar IP público da internet

### Arquitetura

```
┌─────────────────┐                          ┌─────────────────┐
│   Casa A        │                          │   Casa B        │
│   (SERVIDOR)    │                          │   (CLIENTE)     │
│                 │                          │                 │
│  IP Local:      │                          │  IP Local:      │
│  192.168.1.100  │                          │  192.168.0.50   │
│                 │                          │                 │
│  ┌───────────┐  │                          │                 │
│  │ Roteador  │◄─┼──────────────────────────┼────────────────►│
│  │ (NAT)     │  │      Internet            │                 │
│  └───────────┘  │   (IP Público)           │                 │
│  IP Público:    │                          │  Conecta em:    │
│  200.150.10.5   │                          │  200.150.10.5   │
│                 │                          │  (Port Forward) │
│  Port Forward:  │                          │                 │
│  5555 → 5555    │                          │                 │
│  5556 → 5556    │                          │                 │
└─────────────────┘                          └─────────────────┘
```

---

### PARTE A: Preparar o Servidor (Casa A)

#### 1. Descobrir IP Público

**Opção A: Via Site**
- Acesse: https://www.whatismyip.com/
- Anote o IP mostrado (ex: `200.150.10.5`)

**Opção B: Via Comando**
```bash
# Windows PowerShell
(Invoke-WebRequest -Uri "https://api.ipify.org").Content

# Linux/Mac
curl https://api.ipify.org
```

✅ **Anote o IP Público:** `200.150.10.5` (exemplo)

#### 2. Configurar Port Forwarding no Roteador

**⚠️ IMPORTANTE:** Cada roteador tem interface diferente. Passos genéricos:

1. **Acessar o Roteador:**
   - Abra navegador
   - Digite: `http://192.168.1.1` ou `http://192.168.0.1`
   - Login: geralmente `admin` / `admin` (veja etiqueta do roteador)

2. **Encontrar "Port Forwarding" ou "Encaminhamento de Portas":**
   - Pode estar em: Avançado → NAT → Port Forwarding
   - Ou: Segurança → Port Forwarding
   - Ou: Firewall → Port Forwarding

3. **Adicionar Regra 1 - Autenticação:**
   - **Nome:** `Pokemon-Auth`
   - **Porta Externa:** `5555`
   - **Porta Interna:** `5555`
   - **Protocolo:** `TCP`
   - **IP Interno:** `192.168.1.100` (IP local do servidor)
   - **Status:** `Ativo` / `Habilitado`

4. **Adicionar Regra 2 - Batalhas:**
   - **Nome:** `Pokemon-Battle`
   - **Porta Externa:** `5556`
   - **Porta Interna:** `5556`
   - **Protocolo:** `TCP`
   - **IP Interno:** `192.168.1.100` (IP local do servidor)
   - **Status:** `Ativo` / `Habilitado`

5. **Salvar e Reiniciar Roteador** (se necessário)

**Exemplos por marca:**

**TP-Link:**
- Advanced → NAT Forwarding → Virtual Servers
- Add → Preencher campos → Save

**D-Link:**
- Advanced → Port Forwarding
- Add Rule → Preencher campos → Apply

**Intelbras:**
- Avançado → NAT → Port Mapping
- Adicionar → Preencher campos → Salvar

**Netgear:**
- Advanced → Advanced Setup → Port Forwarding
- Add Custom Service → Preencher campos → Apply

#### 3. Testar Port Forwarding

**Site de Teste:**
- Acesse: https://www.yougetsignal.com/tools/open-ports/
- Digite seu IP Público: `200.150.10.5`
- Digite porta: `5555`
- Clique "Check"
- ✅ **Resultado esperado:** "Port 5555 is open on 200.150.10.5"

**Se o resultado for "closed":**
- Verifique se o servidor está rodando
- Verifique se o Firewall do Windows não está bloqueando
- Verifique se a regra de Port Forwarding está correta

#### 4. Configurar Firewall do Windows

**Permitir portas no Firewall:**

```powershell
# Abrir PowerShell como Administrador

# Permitir porta 5555 (Autenticação)
netsh advfirewall firewall add rule name="Pokemon Auth Server" dir=in action=allow protocol=TCP localport=5555

# Permitir porta 5556 (Batalhas)
netsh advfirewall firewall add rule name="Pokemon Battle Server" dir=in action=allow protocol=TCP localport=5556
```

**Resultado esperado:**
```
Ok.
```

#### 5. Iniciar Servidores

**Terminal 1 - AuthServer:**
```bash
cd C:\Users\pedro\Desktop\Projeto-Pokemon
mvn exec:java -Dexec.mainClass="backend.network.server.AuthServer"
```

**Terminal 2 - BattleServer:**
```bash
cd C:\Users\pedro\Desktop\Projeto-Pokemon
mvn exec:java -Dexec.mainClass="backend.network.server.BattleServer"
```

**Ou usar ServerMain (interface gráfica):**
```bash
mvn exec:java -Dexec.mainClass="backend.network.server.ServerMain"
```

✅ **Servidor pronto para receber conexões da internet!**

---

### PARTE B: Conectar Cliente via Internet (Casa B)

#### 1. Obter Informações do Servidor

**Você precisa saber (perguntar ao dono do servidor):**
- **IP Público do Servidor:** `200.150.10.5` (exemplo)
- **Porta Auth:** `5555`
- **Porta Battle:** `5556`

#### 2. Iniciar o Jogo

```bash
cd C:\Users\pedro\Desktop\Projeto-Pokemon
mvn exec:java -Dexec.mainClass="app.Main"
```

#### 3. Login Remoto via Internet

1. Na tela de login:
   - **Marcar** ✅ "🌐 Autenticar no Servidor Remoto"
   - **Host do Servidor:** `200.150.10.5` (IP PÚBLICO do servidor)
   - **Porta:** `5555`
   - **Usuário:** Escolha um nome
   - **Senha:** Escolha uma senha

2. Clicar em **"Registrar"** ou **"Entrar"**

**Resultado esperado:**
```
✅ Conectado ao servidor remoto!
✅ Autenticação bem-sucedida!
```

#### 4. Iniciar Batalha Multiplayer

1. Selecione 5 Pokémons
2. Clique em **"INICIAR BATALHA"**
3. Escolha **"🌐 Modo Multiplayer"**
4. Configure:
   - **Host:** `200.150.10.5` (IP PÚBLICO do servidor)
   - **Porta:** `5556`
   - Clique **"OK"**

**Resultado esperado:**
```
⏳ Aguardando oponente...
Sala: BATTLE-XXXX
```

#### 5. Batalhar

- Mesma mecânica do Modo LAN
- Turnos alternados
- Timeout de 30 segundos
- Opção de desistir

---

### PARTE C: Alternativa - Usar Ngrok (Sem Port Forwarding)

**Se você NÃO consegue configurar Port Forwarding**, use Ngrok:

#### 1. Baixar e Instalar Ngrok

- Acesse: https://ngrok.com/download
- Baixe para seu sistema operacional
- Extraia o arquivo

#### 2. Criar Túnel para Porta 5555 (Auth)

**Terminal 1:**
```bash
# Navegue até a pasta do ngrok
cd C:\ngrok

# Crie túnel para porta 5555
ngrok tcp 5555
```

**Saída esperada:**
```
Session Status                online
Account                       você@email.com
Version                       3.x.x
Region                        United States (us)
Forwarding                    tcp://0.tcp.ngrok.io:12345 -> localhost:5555
```

✅ **Anote:** `0.tcp.ngrok.io:12345` (host e porta)

#### 3. Criar Túnel para Porta 5556 (Battle)

**Terminal 2:**
```bash
# Novo terminal
cd C:\ngrok

# Crie túnel para porta 5556
ngrok tcp 5556
```

**Saída esperada:**
```
Forwarding                    tcp://1.tcp.ngrok.io:54321 -> localhost:5556
```

✅ **Anote:** `1.tcp.ngrok.io:54321` (host e porta)

#### 4. Iniciar Servidores

**Terminal 3:**
```bash
cd C:\Users\pedro\Desktop\Projeto-Pokemon
mvn exec:java -Dexec.mainClass="backend.network.server.AuthServer"
```

**Terminal 4:**
```bash
cd C:\Users\pedro\Desktop\Projeto-Pokemon
mvn exec:java -Dexec.mainClass="backend.network.server.BattleServer"
```

#### 5. Cliente Conecta via Ngrok

**No cliente (Casa B):**

1. Login:
   - Host: `0.tcp.ngrok.io`
   - Porta: `12345`

2. Batalha:
   - Host: `1.tcp.ngrok.io`
   - Porta: `54321`

**✅ Vantagens do Ngrok:**
- Não precisa configurar roteador
- Não precisa saber IP público
- Funciona atrás de NAT restritivo

**⚠️ Desvantagens:**
- Gratuito tem limite de conexões
- URL/porta mudam a cada execução
- Latência pode ser maior

---

## 🔧 Troubleshooting

### Problema 1: "Connection refused"

**Sintoma:**
```
❌ Erro de conexão: Connection refused
```

**Soluções:**

1. **Verificar se o servidor está rodando:**
   ```bash
   # No servidor, verificar se as portas estão abertas
   netstat -an | findstr "5555 5556"
   ```

2. **Verificar Firewall:**
   - Desabilitar temporariamente para testar
   - Adicionar exceção para portas 5555 e 5556

3. **Verificar IP:**
   - Modo LAN: use IP local (`192.168.x.x`)
   - Modo Internet: use IP público

4. **Verificar Port Forwarding:**
   - Acessar roteador e confirmar regras
   - Testar com https://www.yougetsignal.com/tools/open-ports/

### Problema 2: "Aguardando oponente..." infinito

**Sintoma:** Fica esperando oponente e nada acontece

**Soluções:**

1. **Verificar se há 2 jogadores na fila:**
   - Precisa de pelo menos 2 clientes conectados
   - Ambos devem entrar na fila de batalha

2. **Ver logs do servidor:**
   - Verificar se há mensagens de erro
   - Ver se a fila foi criada corretamente

3. **Reiniciar servidor de batalhas:**
   ```bash
   # Ctrl+C no terminal do BattleServer
   # Iniciar novamente
   mvn exec:java -Dexec.mainClass="backend.network.server.BattleServer"
   ```

### Problema 3: "Timeout" durante batalha

**Sintoma:** Timeout acontece mesmo com oponente jogando

**Soluções:**

1. **Verificar latência:**
   ```bash
   # Pingar o servidor
   ping 200.150.10.5
   ```
   - Se latência > 500ms, pode causar timeouts
   - Considere usar servidor mais próximo

2. **Aumentar timeout** (modificar código):
   ```java
   // EnhancedBattlePanel.java, linha ~100
   private static final int TURN_TIMEOUT_SECONDS = 60; // Aumentar para 60s
   ```

3. **Verificar conexão:**
   - Testar velocidade da internet
   - Verificar se não há perda de pacotes

### Problema 4: "Porta já está em uso"

**Sintoma:**
```
java.net.BindException: Address already in use: bind
```

**Soluções:**

1. **Identificar processo usando a porta:**
   ```bash
   # Windows
   netstat -ano | findstr "5555"
   # Anote o PID (última coluna)
   
   # Matar processo
   taskkill /PID <número_do_PID> /F
   ```

2. **Usar porta diferente:**
   - Modificar no servidor
   - Atualizar Port Forwarding
   - Informar cliente da nova porta

### Problema 5: Batalha trava no meio

**Sintoma:** Jogo não responde durante batalha

**Soluções:**

1. **Verificar logs:**
   - Ver exceções no console
   - Verificar se há deadlock

2. **Reiniciar cliente:**
   - Fechar jogo
   - Iniciar novamente
   - Tentar nova batalha

3. **Reiniciar servidor:**
   - Parar ambos os servidores
   - Limpar fila de batalhas (reiniciar)
   - Iniciar novamente

### Problema 6: Imagens não carregam

**Sintoma:** Quadrados brancos no lugar dos Pokémons

**Soluções:**

1. **Verificar pasta Images:**
   ```bash
   # Verificar se existem as pastas
   dir Images\Front-Pokemon
   dir Images\Back-Pokemon
   ```

2. **Recompilar:**
   ```bash
   mvn clean compile
   ```

3. **Verificar cache:**
   - O ImageCache pode estar vazio
   - Reiniciar o jogo para recarregar

### Problema 7: Erro de compilação

**Sintoma:**
```
[ERROR] COMPILATION ERROR
```

**Soluções:**

1. **Limpar e recompilar:**
   ```bash
   mvn clean
   mvn compile
   ```

2. **Verificar versão Java:**
   ```bash
   java -version
   # Precisa ser 17 ou superior
   ```

3. **Atualizar dependências:**
   ```bash
   mvn clean install -U
   ```

---

## ❓ FAQ

### Q1: Posso jogar sozinho offline?

**R:** Sim! Use o **Modo 1: Jogo Local**. Não precisa de internet, servidor ou outro jogador.

---

### Q2: Quantos jogadores podem se conectar ao mesmo tempo?

**R:** O servidor suporta **múltiplos jogadores simultâneos**. Cada batalha é entre 2 jogadores, mas pode haver várias batalhas acontecendo ao mesmo tempo.

---

### Q3: Preciso criar conta para jogar local?

**R:** Não! Para modo local, use as contas padrão:
- admin / admin123
- Ou crie uma conta local nova

---

### Q4: Como sei se estou conectado ao servidor?

**R:** Na tela de login, se você marcou "🌐 Autenticar no Servidor Remoto" e conseguiu fazer login, está conectado. Uma mensagem verde aparece: "✅ Conectado ao servidor remoto!"

---

### Q5: Meu IP público muda toda hora. O que fazer?

**R:** Use um serviço de **DNS Dinâmico** (DDNS):
- No-IP: https://www.noip.com/
- DynDNS: https://dyn.com/
- Ou use Ngrok (não precisa saber o IP)

---

### Q6: Posso usar o mesmo computador como servidor e cliente?

**R:** Sim! Você pode rodar:
- **Terminal 1:** ServerMain (servidores)
- **Terminal 2:** Main (cliente/jogo)

Use `localhost` ou `127.0.0.1` como host no cliente.

---

### Q7: Como salvo meu progresso?

**R:** Os dados são salvos automaticamente em SQLite:
- **Usuários:** `Usuarios.db` (local) ou servidor remoto
- **Pokémons:** `pokedex.db`
- **Times:** Salvos no banco de dados

---

### Q8: Posso mudar as portas 5555 e 5556?

**R:** Sim, mas precisa modificar no código:

```java
// AuthServer.java
private static final int PORT = 7777; // Alterar

// BattleServer.java
private static final int PORT = 8888; // Alterar
```

Depois recompilar e atualizar o Port Forwarding.

---

### Q9: O jogo funciona em celular/tablet?

**R:** Não diretamente. O projeto é Java Desktop (Swing). Para mobile, seria necessário reescrever a interface para Android/iOS.

---

### Q10: Como faço para jogar com amigo em outro país?

**R:** Use o **Modo 3: Servidor Internet** com Port Forwarding ou Ngrok. A latência pode ser maior dependendo da distância.

---

### Q11: É seguro abrir portas no roteador?

**R:** Sim, se feito corretamente:
- Use Port Forwarding (não DMZ)
- Abra apenas portas específicas (5555, 5556)
- Use senhas fortes
- Mantenha o servidor atualizado

**Alternativa mais segura:** Use Ngrok ou VPN.

---

### Q12: Posso hospedar em um servidor cloud (AWS, Azure)?

**R:** Sim! Faça o deploy em uma VM cloud:

1. Criar VM (Amazon EC2, Azure VM, Google Cloud)
2. Instalar Java 17
3. Instalar Maven
4. Fazer upload do projeto
5. Compilar: `mvn clean compile`
6. Iniciar servidores
7. Abrir portas 5555 e 5556 no Security Group
8. Clientes usam o IP público da VM

---

### Q13: Como adiciono mais Pokémons?

**R:** Edite o banco de dados `pokedex.db` usando um editor SQLite (DB Browser for SQLite). Adicione novos registros na tabela `pokemon`.

---

### Q14: Posso trocar o idioma durante o jogo?

**R:** Sim! O sistema suporta **5 idiomas**:
- Português (Brasil)
- Inglês (EUA)
- Espanhol (Espanha)
- Francês (França)
- Italiano (Itália)

Use o seletor de idioma na tela inicial.

---

### Q15: Como reporto um bug?

**R:** Abra uma issue no GitHub:
- Descreva o problema
- Passos para reproduzir
- Logs do console
- Sistema operacional

---

## 📊 Resumo dos Comandos

### Comandos do Servidor

```bash
# Compilar projeto
mvn clean compile

# Iniciar servidor de autenticação
mvn exec:java -Dexec.mainClass="backend.network.server.AuthServer"

# Iniciar servidor de batalhas
mvn exec:java -Dexec.mainClass="backend.network.server.BattleServer"

# Iniciar interface gráfica do servidor (ambos)
mvn exec:java -Dexec.mainClass="backend.network.server.ServerMain"

# Verificar IP local
ipconfig  # Windows
ifconfig  # Linux/Mac

# Verificar portas abertas
netstat -an | findstr "5555 5556"  # Windows
netstat -an | grep "5555\|5556"    # Linux/Mac

# Abrir portas no Firewall (Windows - como Admin)
netsh advfirewall firewall add rule name="Pokemon Auth" dir=in action=allow protocol=TCP localport=5555
netsh advfirewall firewall add rule name="Pokemon Battle" dir=in action=allow protocol=TCP localport=5556
```

### Comandos do Cliente

```bash
# Compilar projeto
mvn clean compile

# Iniciar jogo
mvn exec:java -Dexec.mainClass="app.Main"

# Testar conexão com servidor
ping 192.168.1.100  # LAN
ping 200.150.10.5   # Internet
```

### Comandos Ngrok

```bash
# Túnel para porta 5555 (Auth)
ngrok tcp 5555

# Túnel para porta 5556 (Battle) - novo terminal
ngrok tcp 5556
```

---

## 🎯 Checklist Final

### Antes de Iniciar Batalha Multiplayer

**Servidor:**
- [ ] Projeto compilado (`mvn clean compile`)
- [ ] AuthServer rodando (porta 5555)
- [ ] BattleServer rodando (porta 5556)
- [ ] IP anotado (local para LAN, público para Internet)
- [ ] Port Forwarding configurado (se Internet)
- [ ] Firewall permite conexões nas portas
- [ ] Portas testadas e abertas

**Cliente 1:**
- [ ] Projeto compilado
- [ ] Jogo iniciado
- [ ] Login remoto bem-sucedido
- [ ] 5 Pokémons selecionados
- [ ] Modo Multiplayer escolhido
- [ ] Host e porta corretos configurados
- [ ] Entrou na fila de batalha

**Cliente 2:**
- [ ] Projeto compilado
- [ ] Jogo iniciado
- [ ] Login remoto bem-sucedido
- [ ] 5 Pokémons selecionados
- [ ] Modo Multiplayer escolhido
- [ ] Host e porta corretos configurados
- [ ] Entrou na fila de batalha

**Sistema:**
- [ ] Jogadores pareados automaticamente
- [ ] Sala de batalha criada
- [ ] Turnos alternados funcionando
- [ ] Ataques sendo sincronizados
- [ ] HP atualizando corretamente
- [ ] Desistir funcionando
- [ ] Timeout (30s) funcionando
- [ ] Fim de batalha retorna à Pokédex

---

## 🏆 Conclusão

Este guia cobre **todos os cenários** de execução do Projeto Pokémon:

✅ **Modo Local:** Jogue sozinho contra IA, sem complicações  
✅ **Modo LAN:** Jogue com amigos na mesma rede  
✅ **Modo Internet:** Jogue com qualquer pessoa no mundo  

**Dicas Finais:**

1. **Comece pelo Modo Local** para entender a mecânica
2. **Teste no Modo LAN** antes de configurar Internet
3. **Use Ngrok** se tiver dificuldade com Port Forwarding
4. **Mantenha os servidores atualizados** com `mvn clean compile`
5. **Consulte a seção Troubleshooting** se houver problemas

---

**🎮 Divirta-se jogando Pokémon! 🎮**

---

**Documento gerado em:** 20 de Outubro de 2025  
**Versão do Projeto:** 1.0-SNAPSHOT  
**Autor:** Projeto Pokémon Team  
**Licença:** MIT

---

## 📚 Documentos Relacionados

- [ARCHITECTURE.md](ARCHITECTURE.md) - Arquitetura do sistema
- [SOCKET_INTEGRATION_GUIDE.md](SOCKET_INTEGRATION_GUIDE.md) - Detalhes da comunicação em rede
- [PERFORMANCE_ANALYSIS.md](PERFORMANCE_ANALYSIS.md) - Análise de performance
- [BUG_FIXES_AND_I18N_COMPLETE.md](BUG_FIXES_AND_I18N_COMPLETE.md) - Correções e internacionalização
- [README.md](README.md) - Visão geral do projeto

---

## 📞 Suporte

- **GitHub Issues:** [github.com/pedrowilian/Pokemon-Project/issues](https://github.com/pedrowilian/Pokemon-Project/issues)
- **Email:** (adicionar email se aplicável)
- **Discord:** (adicionar servidor Discord se aplicável)

---

**Happy Gaming! 🎉**
