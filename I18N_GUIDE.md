# Guia de Internacionalização (i18n)

## Visão Geral

O sistema de internacionalização foi implementado usando **ResourceBundle** do Java, permitindo que a aplicação suporte múltiplos idiomas de forma fácil e escalável.

## Arquitetura

### Classe `I18n`
Localização: [frontend/util/I18n.java](src/main/java/frontend/util/I18n.java)

Esta classe gerencia todo o sistema de internacionalização:
- Carrega os arquivos de recursos automaticamente
- Fornece métodos para obter strings traduzidas
- Permite trocar o idioma em tempo de execução
- Suporta formatação de strings com parâmetros

### Arquivos de Recursos
Localização: `src/main/resources/messages*.properties`

- **messages.properties** - Arquivo base (fallback) em Português (BR)
- **messages_pt_BR.properties** - Português do Brasil
- **messages_en_US.properties** - Inglês dos EUA

## Como Usar

### 1. Obter uma string traduzida simples

```java
String titulo = I18n.get("pokedex.title");
// Retorna: "Pokédex"
```

### 2. Obter uma string traduzida com parâmetros

```java
String mensagem = I18n.get("pokedex.status.selected", "Pikachu", 25);
// Retorna: "Pokémon selecionado: Pikachu (ID: 25)"
```

### 3. Mudar o idioma da aplicação

```java
import java.util.Locale;

// Mudar para inglês
I18n.setLocale(Locale.of("en", "US"));

// Mudar para português
I18n.setLocale(Locale.of("pt", "BR"));
```

### 4. Obter idioma atual

```java
Locale currentLocale = I18n.getCurrentLocale();
System.out.println("Idioma atual: " + currentLocale.getDisplayName());
```

### 5. Listar idiomas disponíveis

```java
Locale[] locales = I18n.getAvailableLocales();
for (Locale locale : locales) {
    System.out.println(I18n.getLocaleDisplayName(locale));
}
```

## Exemplo Prático - PokedexPanel

### Antes (hard-coded)
```java
controlPanel.add(UIUtils.createLabel("ID:"), gbc);
statusBar.setText("Pronto");
showError("Por favor, digite um ID.");
```

### Depois (internacionalizado)
```java
controlPanel.add(UIUtils.createLabel(I18n.get("pokedex.label.id")), gbc);
statusBar.setText(I18n.get("pokedex.status.ready"));
showError(I18n.get("pokedex.error.idEmpty"));
```

## Estrutura das Chaves de Tradução

As chaves seguem um padrão hierárquico:

```
[componente].[categoria].[elemento]
```

### Exemplos:

- `pokedex.button.search` - Botão de busca do Pokédex
- `pokedex.error.idInvalid` - Mensagem de erro de ID inválido
- `pokedex.status.ready` - Status "Pronto" da barra de status
- `common.ok` - Texto comum "OK"
- `admin.title` - Título do painel admin

## Como Adicionar um Novo Idioma

### Passo 1: Criar arquivo de recursos

Crie um novo arquivo em `src/main/resources/`:

```
messages_[idioma]_[PAÍS].properties
```

Exemplos:
- Espanhol: `messages_es_ES.properties`
- Francês: `messages_fr_FR.properties`
- Japonês: `messages_ja_JP.properties`

### Passo 2: Copiar todas as chaves

Copie todas as chaves de `messages.properties` e traduza os valores:

```properties
# messages_es_ES.properties
pokedex.title=Pokédex
pokedex.button.search=Buscar
pokedex.button.exit=Salir
pokedex.error.idEmpty=Por favor, ingrese un ID.
```

### Passo 3: Registrar o novo idioma

Edite a classe `I18n.java` e adicione o novo locale:

```java
public static Locale[] getAvailableLocales() {
    return new Locale[] {
        Locale.of("pt", "BR"),  // Portuguese (Brazil)
        Locale.of("en", "US"),  // English (US)
        Locale.of("es", "ES")   // Spanish (Spain) - NOVO!
    };
}
```

### Passo 4: Testar

```java
I18n.setLocale(Locale.of("es", "ES"));
System.out.println(I18n.get("pokedex.title")); // Deve exibir: Pokédex
```

## Como Adicionar Novas Chaves de Tradução

### Passo 1: Adicionar em TODOS os arquivos `.properties`

```properties
# messages.properties
pokedex.newFeature.title=Novo Recurso

# messages_pt_BR.properties
pokedex.newFeature.title=Novo Recurso

# messages_en_US.properties
pokedex.newFeature.title=New Feature
```

### Passo 2: Usar no código

```java
String titulo = I18n.get("pokedex.newFeature.title");
```

## Formatação de Strings com Parâmetros

Use `%s` para strings, `%d` para números inteiros, `%f` para decimais:

```properties
# No arquivo .properties
pokedex.message.captured=Você capturou %s no nível %d!

# No código Java
String msg = I18n.get("pokedex.message.captured", "Pikachu", 5);
// Resultado: "Você capturou Pikachu no nível 5!"
```

### Múltiplos parâmetros

```properties
pokedex.status.filtering=Filtrando por Type: %s, HP >= %d, Attack >= %d, Defense >= %d
```

```java
String status = I18n.get("pokedex.status.filtering", "Fire", 50, 60, 70);
```

## Boas Práticas

### ✅ FAÇA:

1. **Use chaves descritivas e hierárquicas**
   ```java
   I18n.get("pokedex.button.search")  // ✓ BOM
   ```

2. **Agrupe chaves relacionadas**
   ```properties
   pokedex.button.search=...
   pokedex.button.clear=...
   pokedex.button.exit=...
   ```

3. **Use parâmetros para valores dinâmicos**
   ```java
   I18n.get("pokemon.selected", name, id)  // ✓ BOM
   ```

4. **Mantenha todos os arquivos .properties sincronizados**
   - Todas as chaves devem existir em todos os idiomas

5. **Use caracteres Unicode escapados quando necessário**
   ```properties
   # Para caracteres especiais
   pokedex.title=Pok\u00e9dex
   ```

### ❌ NÃO FAÇA:

1. **Evite strings hard-coded**
   ```java
   setText("Buscar");  // ✗ RUIM
   setText(I18n.get("pokedex.button.search"));  // ✓ BOM
   ```

2. **Não concatene strings traduzidas**
   ```java
   // ✗ RUIM
   String msg = I18n.get("hello") + " " + username;

   // ✓ BOM
   String msg = I18n.get("hello.username", username);
   ```

3. **Não use chaves genéricas demais**
   ```java
   I18n.get("button1")  // ✗ RUIM
   I18n.get("pokedex.button.search")  // ✓ BOM
   ```

## Adicionar Seletor de Idioma na Interface

Para permitir que o usuário troque o idioma pela interface:

```java
// Criar um ComboBox com os idiomas disponíveis
JComboBox<String> languageSelector = new JComboBox<>();
for (Locale locale : I18n.getAvailableLocales()) {
    languageSelector.addItem(I18n.getLocaleDisplayName(locale));
}

// Listener para mudar o idioma
languageSelector.addActionListener(e -> {
    int index = languageSelector.getSelectedIndex();
    Locale selectedLocale = I18n.getAvailableLocales()[index];
    I18n.setLocale(selectedLocale);

    // Recarregar a interface
    refreshUI();
});
```

## Status de Internacionalização

### ✅ Componentes Internacionalizados:
- [x] **PokedexPanel** - Completamente internacionalizado
- [ ] LoginFrame - Pendente
- [ ] AdminFrame - Pendente
- [ ] TeamSelectionPanel - Pendente
- [ ] EnhancedBattlePanel - Pendente

### 📋 Próximos Passos:

1. Internacionalizar LoginFrame
2. Internacionalizar AdminFrame
3. Internacionalizar TeamSelectionPanel
4. Internacionalizar EnhancedBattlePanel
5. Adicionar mais idiomas (Espanhol, Francês, etc.)
6. Criar interface para seleção de idioma
7. Salvar preferência de idioma do usuário

## Referências

- [Java ResourceBundle Documentation](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/ResourceBundle.html)
- [Java Locale Documentation](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Locale.html)
- [Classe I18n](src/main/java/frontend/util/I18n.java)

## Suporte

Para questões ou problemas relacionados à internacionalização, verifique:
1. Os logs da aplicação para erros de carregamento de recursos
2. Se todas as chaves existem em todos os arquivos `.properties`
3. Se os arquivos `.properties` estão em `src/main/resources/`
4. Se o encoding dos arquivos está correto (UTF-8)
