# 📘 Pokémon Battle System - Documentação Técnica Completa

**Versão:** 3.0  
**Data:** 20 de Outubro de 2025  
**Autor:** Pedro Wilian

---

## 📑 Índice

1. [Visão Geral da Arquitetura](#1-visão-geral-da-arquitetura)
2. [Estrutura de Pacotes](#2-estrutura-de-pacotes)
3. [Camadas do Sistema](#3-camadas-do-sistema)
4. [Documentação de Classes](#4-documentação-de-classes)
5. [API Reference](#5-api-reference)
6. [Protocolo de Rede](#6-protocolo-de-rede)
7. [Banco de Dados](#7-banco-de-dados)
8. [Configuração e Deployment](#8-configuração-e-deployment)
9. [Padrões de Projeto](#9-padrões-de-projeto)
10. [Performance e Otimizações](#10-performance-e-otimizações)

---

## 1. Visão Geral da Arquitetura

### 1.1 Arquitetura Cliente-Servidor

```
┌─────────────────────────────────────────────────────────────┐
│                         SERVIDOR                             │
│  ┌─────────────────┐              ┌─────────────────┐       │
│  │  AuthServer     │              │  BattleServer   │       │
│  │  (porta 5555)   │              │  (porta 5556)   │       │
│  │                 │              │                 │       │
│  │ - Login/Registro│              │ - Matchmaking   │       │
│  │ - Autenticação  │              │ - BattleRooms   │       │
│  │ - Rate Limiting │              │ - Sincronização │       │
│  └─────────────────┘              └─────────────────┘       │
│           │                                │                 │
│           └────────────────┬───────────────┘                 │
└────────────────────────────┼─────────────────────────────────┘
                             │
                    ┌────────┴────────┐
                    │   REDE (TCP)    │
                    └────────┬────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
┌────────┴────────┐  ┌───────┴────────┐  ┌──────┴────────┐
│   CLIENTE 1     │  │   CLIENTE 2    │  │  CLIENTE N    │
│                 │  │                │  │               │
│ ┌─────────────┐ │  │ ┌────────────┐ │  │ ┌───────────┐ │
│ │  Frontend   │ │  │ │  Frontend  │ │  │ │ Frontend  │ │
│ │  (Swing)    │ │  │ │  (Swing)   │ │  │ │  (Swing)  │ │
│ └──────┬──────┘ │  │ └─────┬──────┘ │  │ └─────┬─────┘ │
│        │        │  │       │        │  │       │       │
│ ┌──────▼──────┐ │  │ ┌─────▼──────┐ │  │ ┌─────▼─────┐ │
│ │  Backend    │ │  │ │  Backend   │ │  │ │  Backend  │ │
│ │  (Services) │ │  │ │  (Services)│ │  │ │ (Services)│ │
│ └──────┬──────┘ │  │ └─────┬──────┘ │  │ └─────┬─────┘ │
│        │        │  │       │        │  │       │       │
│ ┌──────▼──────┐ │  │ ┌─────▼──────┐ │  │ ┌─────▼─────┐ │
│ │   Network   │ │  │ │   Network  │ │  │ │  Network  │ │
│ │   Client    │ │  │ │   Client   │ │  │ │  Client   │ │
│ └─────────────┘ │  │ └────────────┘ │  │ └───────────┘ │
└─────────────────┘  └────────────────┘  └───────────────┘
```

### 1.2 Camadas e Responsabilidades

| Camada | Responsabilidade | Tecnologias |
|--------|------------------|-------------|
| **Apresentação (Frontend)** | UI, eventos, visualização | Swing, AWT |
| **Aplicação (Backend/Application)** | Lógica de negócio, DTOs, serviços | Java Core |
| **Domínio (Backend/Domain)** | Modelos, regras de negócio | Java Core |
| **Infraestrutura (Backend/Infrastructure)** | BD, cache, persistência | SQLite, HikariCP, Caffeine |
| **Rede (Backend/Network)** | Comunicação cliente-servidor | Java Sockets, ExecutorService |
| **Compartilhado (Shared)** | Utilitários, i18n | Java Core |

---

## 2. Estrutura de Pacotes

### 2.1 Estrutura Completa

```
src/main/java/
├── app/                           # Ponto de entrada
│   └── Main.java                  # Classe principal
│
├── frontend/                      # Camada de apresentação
│   ├── controller/                # Controladores MVC
│   │   ├── AdminController.java
│   │   ├── BattleController.java
│   │   ├── LoginController.java
│   │   ├── PokedexController.java
│   │   ├── TeamSelectionController.java
│   │   └── WelcomeController.java
│   │
│   ├── util/                      # Utilitários de UI
│   │   ├── LanguageSelectorComponent.java
│   │   └── UIUtils.java
│   │
│   └── view/                      # Views (telas)
│       ├── AdminFrame.java
│       ├── EnhancedBattlePanel.java
│       ├── LoginFrame.java
│       ├── PokedexPanel.java
│       ├── PokemonUtils.java
│       ├── TeamSelectionPanel.java
│       └── WelcomeFrame.java
│
├── backend/                       # Camada de negócio
│   ├── application/               # Camada de aplicação
│   │   ├── dto/                   # Data Transfer Objects
│   │   │   ├── BattleStateDTO.java
│   │   │   ├── PokemonDTO.java
│   │   │   └── UserDTO.java
│   │   │
│   │   └── service/               # Serviços de aplicação
│   │       ├── BattleService.java
│   │       ├── PokemonService.java
│   │       ├── TeamService.java
│   │       └── UserService.java
│   │
│   ├── domain/                    # Camada de domínio
│   │   ├── model/                 # Entidades de domínio
│   │   │   ├── BattleState.java
│   │   │   ├── Move.java
│   │   │   ├── Pokemon.java
│   │   │   ├── Team.java
│   │   │   ├── Type.java
│   │   │   └── User.java
│   │   │
│   │   └── service/               # Serviços de domínio
│   │       ├── BattleMechanics.java
│   │       ├── DamageCalculator.java
│   │       └── TypeEffectiveness.java
│   │
│   ├── infrastructure/            # Camada de infraestrutura
│   │   ├── ServiceLocator.java
│   │   │
│   │   ├── database/              # Acesso a dados
│   │   │   ├── ConnectionPool.java
│   │   │   ├── DatabaseManager.java
│   │   │   ├── PokedexDAO.java
│   │   │   └── UserDAO.java
│   │   │
│   │   ├── persistence/           # Cache e persistência
│   │   │   ├── CacheManager.java
│   │   │   ├── ImageCache.java
│   │   │   └── PokemonCache.java
│   │   │
│   │   └── security/              # Segurança
│   │       ├── AuthenticationService.java
│   │       ├── PasswordEncryption.java
│   │       └── RateLimiter.java
│   │
│   └── network/                   # Camada de rede
│       ├── client/                # Cliente de rede
│       │   ├── AuthClient.java
│       │   ├── BattleClient.java
│       │   └── NetworkClient.java
│       │
│       ├── protocol/              # Protocolo de comunicação
│       │   ├── Message.java
│       │   ├── MessageType.java
│       │   └── Protocol.java
│       │
│       └── server/                # Servidor
│           ├── AuthServer.java
│           ├── BattleServer.java
│           ├── BattleRoom.java
│           ├── ClientHandler.java
│           └── ServerMain.java
│
└── shared/                        # Componentes compartilhados
    └── util/                      # Utilitários gerais
        ├── CryptoDummy.java
        ├── DateUtils.java
        ├── I18n.java
        ├── MoveTranslator.java
        ├── ReadTextFile.java
        └── TypeTranslator.java
```

---

## 3. Camadas do Sistema

### 3.1 Frontend (Apresentação)

#### 3.1.1 Controllers

**Responsabilidade:** Intermediar View e Backend, gerenciar eventos

| Classe | Descrição |
|--------|-----------|
| `AdminController` | Gerencia painel administrativo |
| `BattleController` | Controla lógica de batalha na UI |
| `LoginController` | Gerencia autenticação de usuários |
| `PokedexController` | Controla navegação da Pokédex |
| `TeamSelectionController` | Gerencia seleção de times |
| `WelcomeController` | Controla tela de boas-vindas |

#### 3.1.2 Views

**Responsabilidade:** Renderizar interface gráfica

| Classe | Descrição |
|--------|-----------|
| `LoginFrame` | Tela de login/registro |
| `WelcomeFrame` | Tela inicial pós-login |
| `PokedexPanel` | Pokédex interativa |
| `TeamSelectionPanel` | Seleção de 5 Pokémons |
| `EnhancedBattlePanel` | Tela de batalha em tempo real |
| `AdminFrame` | Painel de administração |

### 3.2 Backend (Negócio)

#### 3.2.1 Application Layer

**DTOs:**
- `PokemonDTO`: Transferência de dados de Pokémon
- `BattleStateDTO`: Estado da batalha serializado
- `UserDTO`: Dados de usuário sem senha

**Services:**
- `PokemonService`: CRUD de Pokémons
- `BattleService`: Gerencia batalhas (local e multiplayer)
- `TeamService`: Valida e gerencia times
- `UserService`: CRUD de usuários

#### 3.2.2 Domain Layer

**Models:**
- `Pokemon`: Entidade Pokémon (ID, nome, tipos, stats)
- `Move`: Movimento (nome, tipo, poder, PP)
- `Type`: Enum de tipos (NORMAL, FIRE, WATER...)
- `BattleState`: Estado completo da batalha
- `Team`: Time de até 6 Pokémons
- `User`: Entidade de usuário

**Domain Services:**
- `BattleMechanics`: Regras de batalha
- `DamageCalculator`: Cálculo de dano
- `TypeEffectiveness`: Efetividade de tipos

#### 3.2.3 Infrastructure Layer

**Database:**
- `ConnectionPool`: Pool HikariCP (10 conexões)
- `DatabaseManager`: Gerenciador principal
- `PokedexDAO`: Acesso a pokedex.db
- `UserDAO`: Acesso a Usuarios.db

**Persistence:**
- `CacheManager`: Gerenciador de cache Caffeine
- `PokemonCache`: Cache de Pokémons (1000 itens, 30min)
- `ImageCache`: Cache de imagens (500 itens, 2h)

**Security:**
- `AuthenticationService`: Autenticação local/remota
- `PasswordEncryption`: SHA-256 hashing
- `RateLimiter`: 30 requisições/minuto

#### 3.2.4 Network Layer

**Client:**
- `NetworkClient`: Cliente base
- `AuthClient`: Autenticação remota
- `BattleClient`: Conexão com BattleServer

**Protocol:**
- `Message`: Estrutura de mensagem
- `MessageType`: Enum de tipos (AUTH, BATTLE_START, MOVE...)
- `Protocol`: Serialização/deserialização

**Server:**
- `AuthServer`: Servidor de autenticação (porta 5555)
- `BattleServer`: Servidor de batalhas (porta 5556)
- `BattleRoom`: Sala de batalha (2 jogadores)
- `ClientHandler`: Thread por cliente

### 3.3 Shared (Compartilhado)

**Utilities:**
- `I18n`: Internacionalização (5 idiomas)
- `TypeTranslator`: Tradução de tipos
- `MoveTranslator`: Tradução de movimentos
- `DateUtils`: Manipulação de datas
- `ReadTextFile`: Leitura de arquivos
- `CryptoDummy`: Utilitários de criptografia

---

## 4. Documentação de Classes

### 4.1 App Layer

#### 4.1.1 Main.java

```java
package app;

/**
 * Classe principal do sistema.
 * Ponto de entrada da aplicação.
 */
public class Main {
    
    /**
     * Inicia a aplicação mostrando LoginFrame.
     * Configura Look and Feel do sistema.
     * 
     * @param args Argumentos de linha de comando (não utilizados)
     */
    public static void main(String[] args)
}
```

**Responsabilidades:**
- Configurar Look and Feel (FlatLaf ou Nimbus)
- Inicializar ServiceLocator
- Carregar cache de imagens
- Exibir LoginFrame

---

### 4.2 Frontend Layer

#### 4.2.1 LoginFrame.java

```java
package frontend.view;

/**
 * Tela de login e registro de usuários.
 * Permite autenticação local (SQLite) ou remota (AuthServer).
 */
public class LoginFrame extends JFrame {
    
    // Campos principais
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JCheckBox remoteAuthCheckbox;
    private JTextField hostField;
    private JTextField portField;
    
    /**
     * Construtor: inicializa componentes.
     */
    public LoginFrame()
    
    /**
     * Realiza login local ou remoto.
     * Valida credenciais e abre WelcomeFrame.
     */
    private void performLogin()
    
    /**
     * Abre diálogo de registro.
     */
    private void performRegister()
}
```

**Funcionalidades:**
- Login local (SQLite via UserService)
- Login remoto (AuthServer via AuthClient)
- Registro de novos usuários
- Validação de formulários
- Internacionalização completa

---

#### 4.2.2 WelcomeFrame.java

```java
package frontend.view;

/**
 * Tela inicial pós-login.
 * Menu principal com acesso a Pokédex e batalhas.
 */
public class WelcomeFrame extends JFrame {
    
    private User currentUser;
    
    /**
     * Construtor: recebe usuário logado.
     * 
     * @param user Usuário autenticado
     */
    public WelcomeFrame(User user)
    
    /**
     * Abre painel da Pokédex.
     */
    private void openPokedex()
    
    /**
     * Abre seleção de time para batalha.
     */
    private void openTeamSelection()
    
    /**
     * Realiza logout e volta para LoginFrame.
     */
    private void performLogout()
}
```

---

#### 4.2.3 PokedexPanel.java

```java
package frontend.view;

/**
 * Painel interativo da Pokédex.
 * Lista todos os Pokémons com filtros avançados.
 */
public class PokedexPanel extends JPanel {
    
    private JList<Pokemon> pokemonList;
    private PokemonService pokemonService;
    
    /**
     * Construtor: inicializa Pokédex.
     */
    public PokedexPanel()
    
    /**
     * Carrega lista de Pokémons (com cache).
     */
    private void loadPokemonList()
    
    /**
     * Filtra Pokémons por nome/tipo/ID.
     * 
     * @param filterText Texto do filtro
     */
    private void filterPokemons(String filterText)
    
    /**
     * Exibe detalhes do Pokémon selecionado.
     * 
     * @param pokemon Pokémon a exibir
     */
    private void displayPokemonDetails(Pokemon pokemon)
}
```

**Features:**
- Carregamento com cache (PokemonCache)
- Filtros: nome, tipo, ID, geração
- Visualização de stats (HP, ATK, DEF...)
- Imagens front/back/shiny
- Lista de movimentos

---

#### 4.2.4 TeamSelectionPanel.java

```java
package frontend.view;

/**
 * Painel de seleção de time (5 Pokémons).
 * Permite escolher entre modo local ou multiplayer.
 */
public class TeamSelectionPanel extends JPanel {
    
    private List<Pokemon> selectedTeam;
    private static final int MAX_TEAM_SIZE = 5;
    
    /**
     * Construtor: inicializa seleção de time.
     */
    public TeamSelectionPanel()
    
    /**
     * Adiciona Pokémon ao time (max 5).
     * 
     * @param pokemon Pokémon a adicionar
     */
    private void addToTeam(Pokemon pokemon)
    
    /**
     * Remove Pokémon do time.
     * 
     * @param pokemon Pokémon a remover
     */
    private void removeFromTeam(Pokemon pokemon)
    
    /**
     * Mostra diálogo de escolha de modo de batalha.
     * Usa SwingUtilities.invokeLater para evitar bugs.
     */
    private void showBattleModeDialog()
    
    /**
     * Inicia batalha local contra IA.
     */
    private void startLocalBattle()
    
    /**
     * Mostra configuração de servidor multiplayer.
     */
    private void showMultiplayerConfig()
    
    /**
     * Conecta ao BattleServer e aguarda oponente.
     * 
     * @param host Host do servidor
     * @param port Porta do servidor
     */
    private void connectToMultiplayer(String host, int port)
}
```

**Funcionalidades:**
- Seleção visual de até 5 Pokémons
- Validação de time completo
- Modo Local: cria IA automaticamente
- Modo Multiplayer: conecta ao BattleServer
- Tratamento de erros de conexão

---

#### 4.2.5 EnhancedBattlePanel.java

```java
package frontend.view;

/**
 * Painel de batalha em tempo real.
 * Suporta batalhas locais e multiplayer.
 */
public class EnhancedBattlePanel extends JPanel {
    
    private BattleState battleState;
    private BattleClient battleClient;
    private boolean isMultiplayer;
    
    /**
     * Construtor para batalha local.
     * 
     * @param playerTeam Time do jogador
     * @param opponentTeam Time da IA
     */
    public EnhancedBattlePanel(List<Pokemon> playerTeam, List<Pokemon> opponentTeam)
    
    /**
     * Construtor para batalha multiplayer.
     * 
     * @param battleClient Cliente conectado
     * @param battleState Estado inicial da batalha
     */
    public EnhancedBattlePanel(BattleClient battleClient, BattleState battleState)
    
    /**
     * Executa movimento do jogador (local ou envia ao servidor).
     * 
     * @param moveIndex Índice do movimento (0-3)
     */
    private void executePlayerMove(int moveIndex)
    
    /**
     * Processa movimento recebido do servidor.
     * 
     * @param message Mensagem do servidor
     */
    private void processServerMove(Message message)
    
    /**
     * Atualiza interface com novo estado.
     * 
     * @param newState Novo estado da batalha
     */
    private void updateBattleUI(BattleState newState)
    
    /**
     * Inicia timer de turno (30s).
     */
    private void startTurnTimeout()
    
    /**
     * Processa desistência da batalha.
     */
    private void forfeitBattle()
    
    /**
     * Exibe resultado final (vitória/derrota).
     * 
     * @param winner "PLAYER" ou "OPPONENT"
     */
    private void displayBattleResult(String winner)
}
```

**Funcionalidades:**
- Exibição de Pokémons atuais
- 4 botões de movimentos
- Animações de dano/cura
- Log de batalha em tempo real
- Timer de turno (30s para multiplayer)
- Botão de desistir
- Sincronização de estado (multiplayer)

---

### 4.3 Backend Layer

#### 4.3.1 PokemonService.java

```java
package backend.application.service;

/**
 * Serviço de aplicação para Pokémons.
 * Gerencia CRUD e cache de Pokémons.
 */
public class PokemonService {
    
    private PokedexDAO pokedexDAO;
    private PokemonCache pokemonCache;
    
    /**
     * Construtor: injeta dependências.
     */
    public PokemonService(PokedexDAO dao, PokemonCache cache)
    
    /**
     * Busca Pokémon por ID (com cache).
     * 
     * @param id ID do Pokémon (1-151)
     * @return Pokemon ou null se não encontrado
     */
    public Pokemon getPokemonById(int id)
    
    /**
     * Retorna lista de todos os Pokémons (cached).
     * 
     * @return Lista de 151 Pokémons
     */
    public List<Pokemon> getAllPokemons()
    
    /**
     * Busca Pokémons por tipo.
     * 
     * @param type Tipo (FIRE, WATER...)
     * @return Lista de Pokémons do tipo
     */
    public List<Pokemon> getPokemonsByType(Type type)
    
    /**
     * Busca Pokémon por nome.
     * 
     * @param name Nome do Pokémon
     * @return Pokemon ou null
     */
    public Pokemon getPokemonByName(String name)
    
    /**
     * Invalida cache e recarrega.
     */
    public void refreshCache()
}
```

---

#### 4.3.2 BattleService.java

```java
package backend.application.service;

/**
 * Serviço de aplicação para batalhas.
 * Gerencia lógica de batalha local e multiplayer.
 */
public class BattleService {
    
    private BattleMechanics battleMechanics;
    private DamageCalculator damageCalculator;
    
    /**
     * Construtor: injeta serviços de domínio.
     */
    public BattleService(BattleMechanics mechanics, DamageCalculator calculator)
    
    /**
     * Inicia nova batalha local.
     * 
     * @param playerTeam Time do jogador
     * @param opponentTeam Time da IA
     * @return Estado inicial da batalha
     */
    public BattleState startLocalBattle(List<Pokemon> playerTeam, List<Pokemon> opponentTeam)
    
    /**
     * Executa turno de batalha.
     * 
     * @param currentState Estado atual
     * @param playerMove Movimento do jogador
     * @param opponentMove Movimento do oponente
     * @return Novo estado após turno
     */
    public BattleState executeTurn(BattleState currentState, Move playerMove, Move opponentMove)
    
    /**
     * Verifica se batalha terminou.
     * 
     * @param state Estado da batalha
     * @return true se algum time foi derrotado
     */
    public boolean isBattleFinished(BattleState state)
    
    /**
     * Determina vencedor.
     * 
     * @param state Estado final
     * @return "PLAYER", "OPPONENT" ou "DRAW"
     */
    public String getWinner(BattleState state)
    
    /**
     * Gera movimento da IA.
     * 
     * @param currentState Estado atual
     * @return Movimento escolhido pela IA
     */
    public Move generateAIMove(BattleState currentState)
}
```

---

#### 4.3.3 BattleMechanics.java (Domain Service)

```java
package backend.domain.service;

/**
 * Serviço de domínio: regras de batalha Pokémon.
 */
public class BattleMechanics {
    
    /**
     * Calcula dano de um movimento.
     * Fórmula: ((2 * Level / 5 + 2) * Power * A/D / 50 + 2) * Modifiers
     * 
     * @param attacker Pokémon atacante
     * @param defender Pokémon defensor
     * @param move Movimento usado
     * @return Dano calculado
     */
    public int calculateDamage(Pokemon attacker, Pokemon defender, Move move)
    
    /**
     * Determina ordem de ataque (baseado em Speed).
     * 
     * @param pokemon1 Primeiro Pokémon
     * @param pokemon2 Segundo Pokémon
     * @return true se pokemon1 ataca primeiro
     */
    public boolean determineAttackOrder(Pokemon pokemon1, Pokemon pokemon2)
    
    /**
     * Aplica dano ao Pokémon.
     * 
     * @param pokemon Pokémon que recebe dano
     * @param damage Quantidade de dano
     */
    public void applyDamage(Pokemon pokemon, int damage)
    
    /**
     * Verifica se Pokémon está vivo.
     * 
     * @param pokemon Pokémon a verificar
     * @return true se HP > 0
     */
    public boolean isAlive(Pokemon pokemon)
    
    /**
     * Troca Pokémon automaticamente (se atual desmaiou).
     * 
     * @param team Time do jogador
     * @return Próximo Pokémon vivo ou null
     */
    public Pokemon switchPokemon(List<Pokemon> team)
}
```

---

#### 4.3.4 TypeEffectiveness.java (Domain Service)

```java
package backend.domain.service;

/**
 * Serviço de domínio: efetividade de tipos.
 * Implementa tabela de tipos Pokémon Geração 1.
 */
public class TypeEffectiveness {
    
    // Tabela 18x18 de multiplicadores
    private static final double[][] EFFECTIVENESS_TABLE = { /* ... */ };
    
    /**
     * Calcula multiplicador de efetividade.
     * 
     * @param attackType Tipo do ataque
     * @param defenderType1 Primeiro tipo do defensor
     * @param defenderType2 Segundo tipo do defensor (null se single type)
     * @return Multiplicador (0.0, 0.25, 0.5, 1.0, 2.0, 4.0)
     */
    public static double getEffectiveness(Type attackType, Type defenderType1, Type defenderType2)
    
    /**
     * Verifica se ataque é super efetivo.
     * 
     * @param multiplier Multiplicador de efetividade
     * @return true se > 1.0
     */
    public static boolean isSuperEffective(double multiplier)
    
    /**
     * Verifica se ataque não é muito efetivo.
     * 
     * @param multiplier Multiplicador de efetividade
     * @return true se < 1.0
     */
    public static boolean isNotVeryEffective(double multiplier)
}
```

---

#### 4.3.5 ConnectionPool.java (Infrastructure)

```java
package backend.infrastructure.database;

/**
 * Pool de conexões usando HikariCP.
 * Gerencia conexões para pokedex.db e Usuarios.db.
 */
public class ConnectionPool {
    
    private HikariDataSource pokedexPool;
    private HikariDataSource usersPool;
    
    /**
     * Inicializa pools com configuração otimizada.
     */
    public ConnectionPool()
    
    /**
     * Obtém conexão do pool da Pokédex.
     * 
     * @return Connection do pool
     * @throws SQLException se pool esgotado
     */
    public Connection getPokedexConnection() throws SQLException
    
    /**
     * Obtém conexão do pool de usuários.
     * 
     * @return Connection do pool
     * @throws SQLException se pool esgotado
     */
    public Connection getUsersConnection() throws SQLException
    
    /**
     * Fecha todos os pools.
     */
    public void close()
    
    /**
     * Configuração do pool HikariCP:
     * - maximumPoolSize: 10
     * - minimumIdle: 5
     * - connectionTimeout: 5000ms
     * - idleTimeout: 300000ms (5min)
     * - maxLifetime: 1800000ms (30min)
     */
    private HikariConfig createHikariConfig(String dbPath)
}
```

---

#### 4.3.6 PokemonCache.java (Infrastructure)

```java
package backend.infrastructure.persistence;

/**
 * Cache de Pokémons usando Caffeine.
 */
public class PokemonCache {
    
    private Cache<Integer, Pokemon> cache;
    
    /**
     * Inicializa cache com configuração:
     * - maximumSize: 1000
     * - expireAfterWrite: 30 minutos
     * - recordStats: true
     */
    public PokemonCache()
    
    /**
     * Obtém Pokémon do cache.
     * 
     * @param id ID do Pokémon
     * @return Pokemon ou null se não cached
     */
    public Pokemon get(int id)
    
    /**
     * Armazena Pokémon no cache.
     * 
     * @param id ID do Pokémon
     * @param pokemon Pokémon a cachear
     */
    public void put(int id, Pokemon pokemon)
    
    /**
     * Invalida cache completo.
     */
    public void invalidateAll()
    
    /**
     * Retorna estatísticas do cache.
     * 
     * @return CacheStats (hit rate, miss rate, evictions)
     */
    public CacheStats getStats()
}
```

---

#### 4.3.7 AuthServer.java (Network)

```java
package backend.network.server;

/**
 * Servidor de autenticação (porta 5555).
 * Gerencia login e registro remoto.
 */
public class AuthServer {
    
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private RateLimiter rateLimiter;
    
    /**
     * Inicia servidor na porta especificada.
     * 
     * @param port Porta (padrão 5555)
     */
    public void start(int port)
    
    /**
     * Aceita conexões de clientes (loop infinito).
     */
    private void acceptClients()
    
    /**
     * Processa requisição de autenticação.
     * 
     * @param message Mensagem do cliente
     * @return Resposta (sucesso/falha)
     */
    private Message handleAuthRequest(Message message)
    
    /**
     * Processa requisição de registro.
     * 
     * @param message Mensagem do cliente
     * @return Resposta (sucesso/falha)
     */
    private Message handleRegisterRequest(Message message)
    
    /**
     * Para servidor e fecha conexões.
     */
    public void stop()
}
```

---

#### 4.3.8 BattleServer.java (Network)

```java
package backend.network.server;

/**
 * Servidor de batalhas (porta 5556).
 * Gerencia matchmaking e salas de batalha.
 */
public class BattleServer {
    
    private ServerSocket serverSocket;
    private Queue<ClientHandler> waitingQueue;
    private List<BattleRoom> activeRooms;
    private ExecutorService threadPool;
    
    /**
     * Inicia servidor na porta especificada.
     * 
     * @param port Porta (padrão 5556)
     */
    public void start(int port)
    
    /**
     * Aceita jogadores e adiciona à fila.
     */
    private void acceptPlayers()
    
    /**
     * Matchmaking: cria sala quando 2 jogadores na fila.
     */
    private void matchPlayers()
    
    /**
     * Cria sala de batalha para 2 jogadores.
     * 
     * @param player1 Primeiro jogador
     * @param player2 Segundo jogador
     * @return BattleRoom criada
     */
    private BattleRoom createBattleRoom(ClientHandler player1, ClientHandler player2)
    
    /**
     * Remove salas finalizadas.
     */
    private void cleanupFinishedRooms()
    
    /**
     * Para servidor e fecha todas as salas.
     */
    public void stop()
}
```

---

#### 4.3.9 BattleRoom.java (Network)

```java
package backend.network.server;

/**
 * Sala de batalha entre 2 jogadores.
 * Gerencia sincronização de estado.
 */
public class BattleRoom implements Runnable {
    
    private ClientHandler player1;
    private ClientHandler player2;
    private BattleState battleState;
    private boolean isActive;
    
    /**
     * Construtor: inicializa sala com 2 jogadores.
     */
    public BattleRoom(ClientHandler p1, ClientHandler p2)
    
    /**
     * Loop principal da sala (thread separada).
     */
    @Override
    public void run()
    
    /**
     * Aguarda movimentos de ambos os jogadores.
     * Timeout de 30 segundos.
     */
    private void waitForMoves()
    
    /**
     * Processa turno com movimentos recebidos.
     */
    private void processTurn()
    
    /**
     * Envia estado atualizado para ambos os jogadores.
     */
    private void broadcastState()
    
    /**
     * Verifica se batalha terminou.
     * 
     * @return true se algum time foi derrotado
     */
    private boolean isBattleFinished()
    
    /**
     * Envia resultado final para jogadores.
     */
    private void sendBattleResult()
    
    /**
     * Fecha sala e desconecta jogadores.
     */
    public void close()
}
```

---

### 4.4 Shared Layer

#### 4.4.1 I18n.java

```java
package shared.util;

/**
 * Classe de internacionalização.
 * Gerencia 5 idiomas: PT-BR, EN-US, ES-ES, FR-FR, IT-IT.
 */
public class I18n {
    
    private static ResourceBundle bundle;
    private static Locale currentLocale;
    
    /**
     * Inicializa com locale padrão do sistema.
     */
    static { /* ... */ }
    
    /**
     * Obtém texto traduzido pela chave.
     * 
     * @param key Chave de tradução
     * @return Texto traduzido ou chave se não encontrado
     */
    public static String get(String key)
    
    /**
     * Obtém texto traduzido com parâmetros.
     * 
     * @param key Chave de tradução
     * @param params Parâmetros para substituir {0}, {1}...
     * @return Texto formatado
     */
    public static String get(String key, Object... params)
    
    /**
     * Define novo idioma.
     * 
     * @param locale Locale (pt_BR, en_US, es_ES, fr_FR, it_IT)
     */
    public static void setLocale(Locale locale)
    
    /**
     * Retorna locale atual.
     * 
     * @return Locale atual
     */
    public static Locale getCurrentLocale()
    
    /**
     * Lista de locales suportados.
     * 
     * @return Array de Locales
     */
    public static Locale[] getSupportedLocales()
}
```

**Arquivos de propriedades:**
- `messages_pt_BR.properties` (500+ chaves)
- `messages_en_US.properties`
- `messages_es_ES.properties`
- `messages_fr_FR.properties`
- `messages_it_IT.properties`

---

## 5. API Reference

### 5.1 PokemonService API

```java
// Buscar Pokémon por ID
Pokemon pikachu = pokemonService.getPokemonById(25);

// Listar todos
List<Pokemon> all = pokemonService.getAllPokemons();

// Filtrar por tipo
List<Pokemon> fireTypes = pokemonService.getPokemonsByType(Type.FIRE);

// Buscar por nome
Pokemon charizard = pokemonService.getPokemonByName("Charizard");

// Refresh cache
pokemonService.refreshCache();
```

### 5.2 BattleService API

```java
// Iniciar batalha local
List<Pokemon> playerTeam = Arrays.asList(pikachu, charizard, blastoise, venusaur, alakazam);
List<Pokemon> aiTeam = generateAITeam();
BattleState initialState = battleService.startLocalBattle(playerTeam, aiTeam);

// Executar turno
Move playerMove = pikachu.getMoves().get(0); // Thunderbolt
Move aiMove = battleService.generateAIMove(initialState);
BattleState newState = battleService.executeTurn(initialState, playerMove, aiMove);

// Verificar fim
if (battleService.isBattleFinished(newState)) {
    String winner = battleService.getWinner(newState);
    System.out.println("Winner: " + winner);
}
```

### 5.3 UserService API

```java
// Registrar usuário
boolean success = userService.registerUser("player1", "password123");

// Autenticar
User user = userService.authenticate("player1", "password123");

// Listar usuários (admin)
List<User> allUsers = userService.getAllUsers();

// Deletar usuário
boolean deleted = userService.deleteUser("player1");
```

### 5.4 Network Client API

```java
// Conectar ao AuthServer
AuthClient authClient = new AuthClient("192.168.1.100", 5555);
authClient.connect();

// Login remoto
Message loginRequest = new Message(MessageType.AUTH_REQUEST, "player1:password123");
Message response = authClient.sendMessage(loginRequest);

// Conectar ao BattleServer
BattleClient battleClient = new BattleClient("192.168.1.100", 5556);
battleClient.connect();

// Enviar time
Message teamMessage = new Message(MessageType.TEAM_SELECTION, teamDTO.toJson());
battleClient.sendMessage(teamMessage);

// Aguardar matchmaking
Message matchFound = battleClient.receiveMessage();

// Enviar movimento
Message moveMessage = new Message(MessageType.PLAYER_MOVE, moveIndex);
battleClient.sendMessage(moveMessage);

// Receber estado atualizado
Message stateUpdate = battleClient.receiveMessage();
BattleStateDTO newState = BattleStateDTO.fromJson(stateUpdate.getPayload());
```

---

## 6. Protocolo de Rede

### 6.1 Estrutura de Mensagem

```java
public class Message implements Serializable {
    private MessageType type;
    private String payload;
    private long timestamp;
    private String senderId;
}
```

### 6.2 Tipos de Mensagem

```java
public enum MessageType {
    // Autenticação
    AUTH_REQUEST,         // Cliente -> AuthServer: "username:password"
    AUTH_RESPONSE,        // AuthServer -> Cliente: "SUCCESS" ou "FAILURE"
    REGISTER_REQUEST,     // Cliente -> AuthServer: "username:password"
    REGISTER_RESPONSE,    // AuthServer -> Cliente: "SUCCESS" ou "FAILURE"
    
    // Matchmaking
    QUEUE_JOIN,           // Cliente -> BattleServer: "username"
    MATCH_FOUND,          // BattleServer -> Cliente: "opponentName"
    TEAM_SELECTION,       // Cliente -> BattleServer: JSON do time
    
    // Batalha
    BATTLE_START,         // BattleServer -> Clientes: BattleStateDTO
    PLAYER_MOVE,          // Cliente -> BattleServer: índice do movimento
    OPPONENT_MOVE,        // BattleServer -> Cliente: movimento do oponente
    STATE_UPDATE,         // BattleServer -> Clientes: BattleStateDTO atualizado
    BATTLE_END,           // BattleServer -> Clientes: "WINNER:playerId"
    FORFEIT,              // Cliente -> BattleServer: desistência
    
    // Controle
    PING,                 // Cliente <-> Servidor: keep-alive
    PONG,                 // Resposta ao ping
    DISCONNECT,           // Desconexão
    ERROR                 // Mensagem de erro
}
```

### 6.3 Fluxo de Comunicação

#### 6.3.1 Autenticação

```
Cliente                          AuthServer
  |                                  |
  |--AUTH_REQUEST------------------>|
  |   Payload: "user:pass"          |
  |                                  |
  |<-AUTH_RESPONSE------------------|
  |   Payload: "SUCCESS:userId"     |
```

#### 6.3.2 Matchmaking

```
Cliente 1        BattleServer        Cliente 2
  |                  |                  |
  |--QUEUE_JOIN----->|                  |
  |                  |<-QUEUE_JOIN------|
  |                  |                  |
  |<-MATCH_FOUND-----|--MATCH_FOUND---->|
  |                  |                  |
  |--TEAM_SELECTION->|                  |
  |                  |<-TEAM_SELECTION--|
```

#### 6.3.3 Batalha em Tempo Real

```
Cliente 1        BattleRoom         Cliente 2
  |                  |                  |
  |<-BATTLE_START----|--BATTLE_START--->|
  |                  |                  |
  |--PLAYER_MOVE---->|                  |
  |                  |<-PLAYER_MOVE-----|
  |                  |                  |
  |<-STATE_UPDATE----|--STATE_UPDATE--->|
  |                  |                  |
  |     (repete até fim da batalha)     |
  |                  |                  |
  |<-BATTLE_END------|--BATTLE_END----->|
```

---

## 7. Banco de Dados

### 7.1 pokedex.db

#### 7.1.1 Tabela: Pokemon

```sql
CREATE TABLE Pokemon (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    type1 TEXT NOT NULL,
    type2 TEXT,
    hp INTEGER NOT NULL,
    attack INTEGER NOT NULL,
    defense INTEGER NOT NULL,
    spAttack INTEGER NOT NULL,
    spDefense INTEGER NOT NULL,
    speed INTEGER NOT NULL,
    generation INTEGER NOT NULL,
    legendary BOOLEAN NOT NULL DEFAULT 0,
    imageUrl TEXT,
    description TEXT
);
```

**Índices:**
```sql
CREATE INDEX idx_pokemon_name ON Pokemon(name);
CREATE INDEX idx_pokemon_type1 ON Pokemon(type1);
CREATE INDEX idx_pokemon_generation ON Pokemon(generation);
```

#### 7.1.2 Tabela: Moves

```sql
CREATE TABLE Moves (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    type TEXT NOT NULL,
    category TEXT NOT NULL, -- PHYSICAL, SPECIAL, STATUS
    power INTEGER,
    accuracy INTEGER,
    pp INTEGER NOT NULL,
    description TEXT
);
```

#### 7.1.3 Tabela: PokemonMoves

```sql
CREATE TABLE PokemonMoves (
    pokemonId INTEGER NOT NULL,
    moveId INTEGER NOT NULL,
    level INTEGER NOT NULL,
    PRIMARY KEY (pokemonId, moveId),
    FOREIGN KEY (pokemonId) REFERENCES Pokemon(id),
    FOREIGN KEY (moveId) REFERENCES Moves(id)
);
```

### 7.2 Usuarios.db

#### 7.2.1 Tabela: Users

```sql
CREATE TABLE Users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL, -- SHA-256 hash
    role TEXT NOT NULL DEFAULT 'USER', -- USER ou ADMIN
    createdAt TEXT NOT NULL,
    lastLogin TEXT,
    wins INTEGER DEFAULT 0,
    losses INTEGER DEFAULT 0,
    draws INTEGER DEFAULT 0
);
```

**Índices:**
```sql
CREATE INDEX idx_users_username ON Users(username);
CREATE INDEX idx_users_role ON Users(role);
```

### 7.3 Consultas Otimizadas

```sql
-- Buscar Pokémon por ID (cached)
SELECT * FROM Pokemon WHERE id = ?;

-- Listar por tipo
SELECT * FROM Pokemon WHERE type1 = ? OR type2 = ? ORDER BY id;

-- Buscar movimentos de um Pokémon
SELECT m.* FROM Moves m
JOIN PokemonMoves pm ON m.id = pm.moveId
WHERE pm.pokemonId = ?
ORDER BY pm.level;

-- Autenticar usuário
SELECT * FROM Users WHERE username = ? AND password = ?;

-- Atualizar estatísticas de batalha
UPDATE Users SET wins = wins + 1, lastLogin = ? WHERE id = ?;
```

---

## 8. Configuração e Deployment

### 8.1 Requisitos de Sistema

**Servidor:**
- ☕ Java 17+
- 💾 2 GB RAM
- 🌐 Portas 5555 e 5556 abertas
- 📦 Maven 3.6+

**Cliente:**
- ☕ Java 17+
- 💾 1 GB RAM
- 🖥️ Resolução mínima: 800x600
- 📦 Maven 3.6+

### 8.2 Configuração de Rede

#### 8.2.1 Firewall (Windows)

```powershell
# Permitir porta 5555 (AuthServer)
New-NetFirewallRule -DisplayName "Pokemon Auth Server" -Direction Inbound -LocalPort 5555 -Protocol TCP -Action Allow

# Permitir porta 5556 (BattleServer)
New-NetFirewallRule -DisplayName "Pokemon Battle Server" -Direction Inbound -LocalPort 5556 -Protocol TCP -Action Allow
```

#### 8.2.2 Port Forwarding (Roteador)

1. Acesse configurações do roteador (geralmente `192.168.1.1`)
2. Navegue até "Port Forwarding" ou "NAT"
3. Adicione regras:
   - **Porta Externa:** 5555 → **Porta Interna:** 5555 → **IP:** <IP_LOCAL_SERVIDOR>
   - **Porta Externa:** 5556 → **Porta Interna:** 5556 → **IP:** <IP_LOCAL_SERVIDOR>

### 8.3 Variáveis de Ambiente

```bash
# Configurar JDK
export JAVA_HOME=/path/to/jdk-17

# Aumentar heap size (servidor)
export MAVEN_OPTS="-Xmx2G -Xms512M"

# Configurar timezone
export TZ=America/Sao_Paulo
```

### 8.4 Compilação

```bash
# Compilar projeto
mvn clean compile

# Executar testes
mvn test

# Criar JAR executável
mvn package

# JAR gerado em: target/pokemon-battle-3.0.jar
```

### 8.5 Execução

#### 8.5.1 Servidor com GUI

```bash
mvn exec:java -Dexec.mainClass="backend.network.server.ServerMain"
```

#### 8.5.2 Cliente

```bash
mvn exec:java -Dexec.mainClass="app.Main"
```

#### 8.5.3 Executar JAR

```bash
# Servidor
java -jar target/pokemon-battle-3.0.jar --server

# Cliente
java -jar target/pokemon-battle-3.0.jar
```

### 8.6 Logs

**Localização:**
- Servidor: `logs/server.log`
- Cliente: `logs/client.log`

**Configuração:**
```properties
# log4j2.properties
rootLogger.level=INFO
appender.file.type=File
appender.file.fileName=logs/server.log
appender.file.layout.pattern=%d{yyyy-MM-dd HH:mm:ss} [%t] %-5level %logger{36} - %msg%n
```

---

## 9. Padrões de Projeto

### 9.1 Padrões Utilizados

| Padrão | Localização | Descrição |
|--------|-------------|-----------|
| **MVC** | `frontend/*` | Separação View/Controller |
| **Service Layer** | `backend/application/service` | Lógica de negócio |
| **Repository (DAO)** | `backend/infrastructure/database` | Acesso a dados |
| **DTO** | `backend/application/dto` | Transferência de dados |
| **Singleton** | `ServiceLocator`, `CacheManager` | Instância única |
| **Factory** | `PokemonFactory` | Criação de objetos |
| **Strategy** | `BattleMechanics` | Algoritmos intercambiáveis |
| **Observer** | Eventos de batalha | Notificações |
| **Command** | Movimentos de batalha | Encapsular ações |
| **Object Pool** | `ConnectionPool` (HikariCP) | Reutilização de recursos |
| **Cache-Aside** | `PokemonCache`, `ImageCache` | Cache otimizado |

### 9.2 Arquitetura em Camadas

```
┌─────────────────────────────────────┐
│      Presentation Layer             │
│  (Views, Controllers, UI Utils)     │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│      Application Layer              │
│  (Services, DTOs, Use Cases)        │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│       Domain Layer                  │
│  (Entities, Domain Services)        │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│    Infrastructure Layer             │
│  (Database, Cache, Network)         │
└─────────────────────────────────────┘
```

### 9.3 Dependency Injection

**ServiceLocator Pattern:**

```java
public class ServiceLocator {
    private static PokemonService pokemonService;
    private static BattleService battleService;
    private static UserService userService;
    
    public static PokemonService getPokemonService() {
        if (pokemonService == null) {
            pokemonService = new PokemonService(
                new PokedexDAO(),
                new PokemonCache()
            );
        }
        return pokemonService;
    }
}
```

---

## 10. Performance e Otimizações

### 10.1 Métricas de Performance

| Operação | Antes | Depois | Melhoria |
|----------|-------|--------|----------|
| **Startup** | 700ms | 100ms | **86%** ⚡ |
| **Carregar Pokédex** | 2500ms | 25ms | **99%** ⚡ |
| **Executar Turno** | 150ms | 0.7ms | **99.5%** ⚡ |
| **Carregar Imagem** | 80ms | 5ms | **94%** ⚡ |
| **Query Pokémon** | 45ms | 0.3ms | **99.3%** ⚡ |

### 10.2 Otimizações Implementadas

#### 10.2.1 Connection Pooling (HikariCP)

**Antes:** Nova conexão a cada query (overhead 40-50ms)
**Depois:** Pool reutilizável (overhead <1ms)

```java
HikariConfig config = new HikariConfig();
config.setMaximumPoolSize(10);
config.setMinimumIdle(5);
config.setConnectionTimeout(5000);
config.setIdleTimeout(300000);
config.setMaxLifetime(1800000);
```

#### 10.2.2 Cache Caffeine

**Pokémon Cache:**
- Tamanho: 1000 itens
- TTL: 30 minutos
- Hit Rate: ~98%

**Image Cache:**
- Tamanho: 500 imagens
- TTL: 2 horas
- Hit Rate: ~95%

```java
Cache<Integer, Pokemon> pokemonCache = Caffeine.newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(30, TimeUnit.MINUTES)
    .recordStats()
    .build();
```

#### 10.2.3 Lazy Loading

**Imagens:** Carregadas sob demanda e cacheadas
**Movimentos:** Carregados apenas quando necessário

#### 10.2.4 Thread Pools

**Servidor:**
```java
ExecutorService threadPool = Executors.newFixedThreadPool(50);
```

**Cliente:**
```java
ExecutorService networkThread = Executors.newSingleThreadExecutor();
```

#### 10.2.5 Índices de Banco de Dados

```sql
CREATE INDEX idx_pokemon_name ON Pokemon(name);
CREATE INDEX idx_pokemon_type1 ON Pokemon(type1);
CREATE INDEX idx_users_username ON Users(username);
```

### 10.3 Profiling

**Ferramentas utilizadas:**
- JProfiler
- VisualVM
- Java Flight Recorder

**Hotspots identificados:**
1. ✅ Queries de banco (resolvido com cache)
2. ✅ Carregamento de imagens (resolvido com cache)
3. ✅ Cálculo de dano (otimizado com lookup tables)

---

## 📚 Referências

### Documentação Oficial
- [Java 17 Documentation](https://docs.oracle.com/en/java/javase/17/)
- [HikariCP GitHub](https://github.com/brettwooldridge/HikariCP)
- [Caffeine Cache](https://github.com/ben-manes/caffeine)
- [SQLite Documentation](https://www.sqlite.org/docs.html)

### Guias do Projeto
- [ARCHITECTURE.md](ARCHITECTURE.md) - Arquitetura detalhada
- [PERFORMANCE_ANALYSIS.md](PERFORMANCE_ANALYSIS.md) - Análise de performance
- [GUIA_COMPLETO_EXECUCAO.md](GUIA_COMPLETO_EXECUCAO.md) - Guia de execução

---

**Última atualização:** 20 de Outubro de 2025  
**Versão:** 3.0  
**Autor:** Pedro Wilian
